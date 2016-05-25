package com.np.listviewdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author NingPan
 * @version $Rev$
 * @time 2016/5/25 10:13
 * @des 使用 findViewWithTag 的方式解决 listView 图片乱序问题.
 */
public class MyBaseAdapter extends BaseAdapter {

    private Context mContext;
    private String[] datas;
    private int layoutId;

    private ListView mListView;

    private LruCache<String, BitmapDrawable> mMemoryCache;

    public MyBaseAdapter(Context context, String[] datas, int layoutId) {
        this.mContext = context;
        this.datas = datas;
        this.layoutId = layoutId;

        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = (maxMemory / 8);
        mMemoryCache = new LruCache<String, BitmapDrawable>(cacheSize) {
            @Override
            protected int sizeOf(String key, BitmapDrawable value) {
                return value.getBitmap().getByteCount() / 1024;
            }
        };
    }

    public void addBitmapToMemoryCache(String key, BitmapDrawable bitmapDrawable) {
        if (mMemoryCache.get(key) == null) {
            mMemoryCache.put(key, bitmapDrawable);
        }
    }

    public BitmapDrawable getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }

    @Override
    public int getCount() {
        return datas.length;
    }

    @Override
    public Object getItem(int position) {
        return datas[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mListView == null) {
            mListView = (ListView) parent;
        }
        View view;
        String imageUrl = datas[position];
        if (convertView == null) {
            view = LayoutInflater.from(mContext).inflate(layoutId, null, false);
        } else {
            view = convertView;
        }
        ImageView imageView = (ImageView) view.findViewById(R.id.item_imageView);
        imageView.setTag(imageUrl);
        BitmapDrawable bitmapDrawable = getBitmapFromMemoryCache(imageUrl);
        if (bitmapDrawable != null) {
            imageView.setImageDrawable(bitmapDrawable);
        } else {
            BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            task.execute(imageUrl);
        }
        return view;
    }

    class BitmapWorkerTask extends AsyncTask<String, Void, BitmapDrawable> {

        private ImageView mImageView;
        private String imageUrl;

        public BitmapWorkerTask(ImageView imageView) {
            this.mImageView = imageView;
        }

        @Override
        protected BitmapDrawable doInBackground(String... params) {
            imageUrl = params[0];
            Bitmap bitmap = downloadBitmap(imageUrl);
            BitmapDrawable bitmapDrawable = new BitmapDrawable(mContext.getResources(), bitmap);
            addBitmapToMemoryCache(imageUrl, bitmapDrawable);
            return bitmapDrawable;
        }

        @Override
        protected void onPostExecute(BitmapDrawable bitmapDrawable) {
            super.onPostExecute(bitmapDrawable);
            ImageView imageView = (ImageView) mListView.findViewWithTag(imageUrl);
            if (imageView != null && bitmapDrawable != null) {
                imageView.setImageDrawable(bitmapDrawable);
            }
        }

        private Bitmap downloadBitmap(String imageUrl) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(imageUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5 * 1000);
                conn.setReadTimeout(10 * 1000);
                return BitmapFactory.decodeStream(conn.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return null;
        }
    }
}
