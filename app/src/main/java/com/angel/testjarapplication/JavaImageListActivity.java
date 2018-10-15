package com.angel.testjarapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.angel.makejar.theta_v.DeviceOp_GetFileList;
import com.angel.makejar.theta_v.ImageInfo;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadmoreListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * 创建日期：2018/9/19 on 10:29
 * 描述:
 * 作者:波波 yjb
 */
public class JavaImageListActivity extends AppCompatActivity {
    public static final int REQUEST_GET_ACCOUNT=112;
    private DeviceOp_GetFileList deviceOp_getFileList;
    private JavaGridViewAdapter adapter;
    private ArrayList<ImageInfo> imageInfos;
    private String fileUrl;
    private ProgressDialog progress;
    private File rootFile,file;
    private SmartRefreshLayout smart_refresh;
    private GridView grid_view;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_list);
        initView();
        initInfo();
        initEvent();
    }

    private void initEvent() {
        smart_refresh.setOnLoadmoreListener(new OnLoadmoreListener() {
            @Override
            public void onLoadmore(RefreshLayout refreshlayout) {
                smart_refresh.finishLoadmore();
            }
        });

        smart_refresh.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                smart_refresh.finishRefresh();
            }
        });

        grid_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                fileUrl=imageInfos.get(i).FilePath;
                file=new File(rootFile.getAbsoluteFile(),fileUrl.substring(fileUrl.lastIndexOf("/")+1,fileUrl.length()));
                if(!checkPermission()){
                    requestPermission();
                }else {
                    if(file.exists()){
                        Toast.makeText(JavaImageListActivity.this,file.getAbsolutePath(),Toast.LENGTH_SHORT).show();
                    }else {
                        doDownLoad();
                    }
                }
            }
        });
    }

    private void initInfo() {
        imageInfos=new ArrayList<>();
        adapter=new JavaGridViewAdapter(JavaImageListActivity.this,imageInfos);
        grid_view.setAdapter(adapter);

        progress=new ProgressDialog(JavaImageListActivity.this);
        progress.setIcon(R.mipmap.ic_launcher);
        progress.setTitle("提示信息");
        progress.setMessage("正在下载，请稍候...");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        rootFile=new File(Environment.getExternalStorageDirectory().toString() + File.separator + "FirstJarTest");
        if(!rootFile.exists()){
            rootFile.mkdirs();
        }

        deviceOp_getFileList=new DeviceOp_GetFileList();
        deviceOp_getFileList.setIResult(new DeviceOp_GetFileList.IResult() {
            @Override
            public void onSuccess(ArrayList<ImageInfo> arrayList, int i) {
                if(i==0)
                    smart_refresh.finishRefresh();
                else
                    smart_refresh.finishLoadmore();

                if(imageInfos.size()>0){
                    imageInfos.clear();
                }
                imageInfos.addAll(arrayList);
                adapter.notifyDataSetChanged();
            }
        });
        getImageList(0);

    }

    private void initView() {
        setTitle("图片列表");
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        smart_refresh=findViewById(R.id.smart_refresh);
        grid_view=findViewById(R.id.grid_view);
    }

    private void getImageList(int start){
        deviceOp_getFileList.startPosition=start;
        deviceOp_getFileList.MaxFileCount=10;
        new Thread(deviceOp_getFileList).start();
    }


    private boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_GET_ACCOUNT);
    }

    private void doDownLoad(){
        new DownLoadTask().execute(fileUrl);
    }

    class DownLoadTask extends AsyncTask<String,Integer,File> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress.show();
            progress.setProgress(0);
        }

        @Override
        protected File doInBackground(String... strings) {
            return getFileFromTheta(strings[0]);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progress.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            progress.cancel();
            Toast.makeText(JavaImageListActivity.this,"下载完成",Toast.LENGTH_SHORT).show();
        }
    }

    private File getFileFromTheta(String fileUrl){
        // 从网络上获取图片
        URL url = null;
        InputStream inputStream=null;
        FileOutputStream fos=null;
        try {
            url = new URL(fileUrl);
            HttpURLConnection conn= (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            if(conn.getResponseCode()==200){
                inputStream=conn.getInputStream();
                fos=new FileOutputStream(file);
                byte[] buffer=new byte[1024];
                int len=0;
                while ((len=inputStream.read(buffer))!=-1){
                    fos.write(buffer,0,len);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(inputStream!=null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(fos!=null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
