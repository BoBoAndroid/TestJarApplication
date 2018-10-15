package com.angel.testjarapplication

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.angel.makejar.theta_v.DeviceOp_GetInfo
import com.angel.makejar.theta_v.ImageCaptureControl
import com.angel.makejar.theta_v.LivePreviewControl
import kotlinx.android.synthetic.main.activity_main_kot.*

class MainActivity : AppCompatActivity() {
    private lateinit var deviceOpGetInfo:DeviceOp_GetInfo
    private lateinit var livePreviewControl : LivePreviewControl
    private lateinit var imageCaptureControl: ImageCaptureControl
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_kot)
        initView()
        initInfo()
        initCamera()
    }

    private fun initView(){
        title="ThetaDemo"
    }

    private fun initInfo(){
        //获取设备信息
        deviceOpGetInfo = DeviceOp_GetInfo()
        deviceOpGetInfo.setIResult { result ->
            runOnUiThread {
                if(result){
                    Toast.makeText(this@MainActivity, "连接成功", Toast.LENGTH_SHORT).show()
                }else {
                    Toast.makeText(this@MainActivity, "没有连接全景相机", Toast.LENGTH_SHORT).show()
                }
            }
        }

        //拍照相关
        imageCaptureControl = ImageCaptureControl()
        imageCaptureControl.setIResult { result ->
            runOnUiThread{
                Toast.makeText(this@MainActivity, "拍照完成$result", Toast.LENGTH_SHORT).show()
                livePreviewControl.begin()
            }
        }


    }


    fun onClick(view:View){
        when(view.id){
            R.id.bt_con_info->{
                initDate()
            }
            R.id.bt_pre->{
                //开始预览
                livePreviewControl.begin()
            }
            R.id.bt_cap->{
                //拍照
                takePicture()
            }
            R.id.bt_jump->{
                activityChange(this, ImageListActivity().javaClass)
            }
        }
    }

    fun activityChange(ctx: Context, clazz:Class<Any>){
        var intent = Intent()
        intent.setClass(ctx,clazz)
        startActivity(intent)
    }

    //判断有没有连接着全景相机
    private fun initDate(){
        Thread(deviceOpGetInfo).start()
    }

    //实例化预览对象，并展示
    private fun initCamera(){
        livePreviewControl = object : LivePreviewControl() {
            override  fun onLiveFrameReceived(bit: Bitmap) {
                super.onLiveFrameReceived(bit)
                val sh = gl_surface.holder
                var canvas: Canvas = sh.lockCanvas()
                canvas.drawBitmap(bit,20f,(resources.displayMetrics.heightPixels / 4).toFloat(),null)
                sh.unlockCanvasAndPost(canvas)
            }
        }
    }

    //拍照方法
    private fun takePicture(){
        livePreviewControl.end()
        imageCaptureControl.begin()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item!!.itemId){
            R.id.action_java->{
                //跳转Java案例
                activityChange(this,DemoJavaActivity().javaClass)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}