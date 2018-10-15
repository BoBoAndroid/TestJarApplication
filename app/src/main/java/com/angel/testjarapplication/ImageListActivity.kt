package com.angel.testjarapplication

import android.Manifest
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.ActionBar
import com.angel.makejar.theta_v.DeviceOp_GetFileList
import com.angel.makejar.theta_v.ImageInfo
import kotlinx.android.synthetic.main.activity_image_list.*
import java.io.File
import android.app.ProgressDialog
import android.os.*
import android.view.MenuItem
import android.widget.Toast
import com.angel.firstjartest.GridViewAdapter
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL


/**
 * 创建日期：2018/9/18 on 9:42
 * 描述:
 * 作者:波波 yjb
 */
class ImageListActivity : AppCompatActivity(){
    private val REQUEST_GET_ACCOUNT = 112
    private lateinit var deviceOpGetFileList: DeviceOp_GetFileList
    private lateinit var gridViewAdapter: GridViewAdapter
    private lateinit var imageInfos: ArrayList<ImageInfo>
    private lateinit var fileUrl : String
    private lateinit var progress: ProgressDialog
    private lateinit var rootFile: File
    private lateinit var file : File
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_list)
        initView()
        initInfo()
        initEvent()
    }

    private fun initEvent(){
        smart_refresh.setOnRefreshListener {
            getImageList(0)
        }
        smart_refresh.setOnLoadmoreListener {
            getImageList(this.imageInfos.size)
        }
        grid_view.setOnItemClickListener { _, _, i, _ ->
            fileUrl= imageInfos[i].FilePath
            file=File(rootFile.absoluteFile,fileUrl.substring(fileUrl.lastIndexOf("/")+1,fileUrl.length))
            if(!checkPermission()) {
                requestPermission()
            }
            else{
                if(file.exists()){
                    Toast.makeText(this@ImageListActivity,file.path,Toast.LENGTH_SHORT).show()
                }else {
                    doDownLoad()
                }
            }
        }
    }

    private fun doDownLoad(){
        DownLoadTask().execute(fileUrl)
    }

    private fun initView(){
        title="图片列表"
        var actionBar : ActionBar= this!!.supportActionBar!!
        if(actionBar!=null){
            actionBar.setHomeButtonEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }

        imageInfos=ArrayList()
        gridViewAdapter= GridViewAdapter(this,imageInfos)
        grid_view.adapter=gridViewAdapter

        progress=ProgressDialog(this)
        progress.setIcon(R.mipmap.ic_launcher)
        progress.setTitle("提示信息")
        progress.setMessage("正在下载，请稍候...")
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)

        rootFile= File(Environment.getExternalStorageDirectory().toString() + File.separator + "FirstJarTest")
        if(!rootFile.exists()){
            rootFile.mkdirs()
        }
    }

    private fun initInfo(){
        //获取图片列表相关
        deviceOpGetFileList = DeviceOp_GetFileList()
        deviceOpGetFileList.setIResult { Result, startPotion ->
            if(startPotion==0) smart_refresh.finishRefresh() else smart_refresh.finishLoadmore()
            if(this.imageInfos.size>0){
                this.imageInfos.clear()
            }
            this.imageInfos.addAll(Result)
            gridViewAdapter.notifyDataSetChanged()
        }

        getImageList(0)
    }

    private fun getImageList(start : Int){
        deviceOpGetFileList.startPosition=start
        deviceOpGetFileList.MaxFileCount=10
        Thread(deviceOpGetFileList).start()
    }


    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_GET_ACCOUNT)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_GET_ACCOUNT->{
                if(grantResults.isNotEmpty() && grantResults[0] ==PackageManager.PERMISSION_GRANTED){
                    if(file.exists()){
                        Toast.makeText(this@ImageListActivity,file.path,Toast.LENGTH_SHORT).show()
                    }else {
                        doDownLoad()
                    }
                }else{
                    requestPermission()
                }
            }
        }
    }

    inner class DownLoadTask : AsyncTask<String, Int, File>() {

        override fun onPreExecute() {
            super.onPreExecute()
            //开始下载 对话框进度条显示
            progress.show()
            progress.progress = 0
        }

        override fun doInBackground(vararg p0: String?): File {
            return getFileFromTheta(p0[0])
        }

        override fun onProgressUpdate(vararg values: Int?) {
            super.onProgressUpdate(*values)
            progress.progress = values[0]!!
        }

        override fun onPostExecute(result: File) {
            super.onPostExecute(result)
            //下载完成 对话框进度条隐藏
            progress.cancel()
            Toast.makeText(this@ImageListActivity,result.path,Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileFromTheta(s: String?): File{
        // 从网络上获取图片
        val url = URL(s)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.requestMethod = "GET"
        conn.doInput = true
        if (conn.responseCode === 200) {

            val inputStream = conn.inputStream
            val fos = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var len: Int
            do {
                len=inputStream.read(buffer)
                if(len!=-1){
                    fos.write(buffer, 0, len) 
                }
            }while (len!=-1)
            inputStream.close()
            fos.close()
        }
        return file
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item!!.itemId){
            android.R.id.home->{
                this.finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}