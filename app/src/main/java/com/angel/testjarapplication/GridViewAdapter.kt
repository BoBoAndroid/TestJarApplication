package com.angel.firstjartest

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.angel.makejar.theta_v.ImageInfo
import com.angel.testjarapplication.R
import com.bumptech.glide.Glide

/**
 * 创建日期：2018/9/18 on 9:51
 * 描述:
 * 作者:波波 yjb
 */
class GridViewAdapter(private var context: Context, private var imageInfos: ArrayList<ImageInfo>) : BaseAdapter(){

    override fun getView(p0: Int, convertView: View?, p2: ViewGroup?): View {
        var holder : ViewHolder
        //重用view
        var v: View
        if (convertView == null) {
            holder = ViewHolder()
            v = LayoutInflater.from(context).inflate(R.layout.list_item_layout, p2, false)
            holder.iv_show = v.findViewById(R.id.iv_show) as ImageView
            //设置tag
            v.tag = holder
        } else {
            v = convertView
            //获取tag并强转
            holder = v.tag as ViewHolder
        }

        Glide.with(context).load(imageInfos[p0].FilePath).placeholder(R.mipmap.ic_launcher).into(holder.iv_show)
        return v!!
    }

    override fun getItem(p0: Int): Any {
        return imageInfos[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getCount(): Int {
        return if(imageInfos.size==0) 0 else imageInfos.size
    }

    inner class ViewHolder{
        lateinit var iv_show : ImageView
    }
}