package com.np.listviewdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;

/**
 * ListView 异步加载网络图片可能出现图片乱序问题：解决方案有三种.
 * ①. 使用 findViewWithTag 方式 :如 MyBaseAdapter.java
 * ②. 使用 弱引用关联的方式    :如 MyBaseAdapter2.java
 * ③. 使用 Volley 下的 NetworkImageView 控件
 */
public class MainActivity extends AppCompatActivity {

    private ListView mListView;
//    private MyBaseAdapter myBaseAdapter;
    private MyBaseAdapter2 myBaseAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (ListView) findViewById(R.id.listView);
//        myBaseAdapter = new MyBaseAdapter(this, Images.imageUrls, R.layout.listview_item);
        myBaseAdapter = new MyBaseAdapter2(this, Images.imageUrls, R.layout.listview_item);
        mListView.setAdapter(myBaseAdapter);
    }
}
