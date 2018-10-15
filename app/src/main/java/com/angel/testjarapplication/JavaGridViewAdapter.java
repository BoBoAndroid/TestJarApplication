package com.angel.testjarapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.angel.makejar.theta_v.ImageInfo;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

/**
 * 创建日期：2018/9/19 on 11:06
 * 描述:
 * 作者:波波 yjb
 */
public class JavaGridViewAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<ImageInfo> imageInfos;
    private LayoutInflater inflater;

    public JavaGridViewAdapter(Context context,ArrayList<ImageInfo> imageInfos){
        this.context=context;
        this.imageInfos=imageInfos;
        inflater=LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return imageInfos.size()==0?0:imageInfos.size();
    }

    @Override
    public Object getItem(int i) {
        return imageInfos.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder=null;
        if(view==null){
            holder=new ViewHolder();
            view=inflater.inflate(R.layout.list_item_layout,null);
            holder.imageView=view.findViewById(R.id.iv_show);
            view.setTag(holder);
        }else {
            holder= (ViewHolder) view.getTag();
        }
        Glide.with(context).load(imageInfos.get(i).FilePath).placeholder(R.mipmap.ic_launcher).into(holder.imageView);
        return view;
    }

    class ViewHolder{
        ImageView imageView;
    }
}
