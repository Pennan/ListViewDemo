package com.np.listviewdemo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author NingPan
 * @version $Rev$
 * @time 2016/5/25 10:13
 * @des ${TODO}
 */
public class MyBaseAdapter2 extends BaseAdapter {

    private Context mContext;
    private String[] datas;
    private int layoutId;

    private Bitmap mLoadBitmap;

    private LruCache<String, BitmapDrawable> mMemoryCache;

    public MyBaseAdapter2(Context context, String[] datas, int layoutId) {
        this.mContext = context;
        this.datas = datas;
        this.layoutId = layoutId;

        mLoadBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.default_bg3);

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
        View view;
        String imageUrl = datas[position];
        if (convertView == null) {
            view = LayoutInflater.from(mContext).inflate(layoutId, null, false);
        } else {
            view = convertView;
        }
        ImageView imageView = (ImageView) view.findViewById(R.id.item_imageView);
        BitmapDrawable bitmapDrawable = getBitmapFromMemoryCache(imageUrl);
        if (bitmapDrawable != null) {
            imageView.setImageDrawable(bitmapDrawable);
        } else if (cancelPotentialWork(imageUrl, imageView)){
            BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            AsyncDrawable drawable = new AsyncDrawable(mContext.getResources(), mLoadBitmap, task);
            imageView.setImageDrawable(drawable);
            task.execute(imageUrl);
        }
        return view;
    }

    /**
     * 取消掉后台的潜在任务,当认为当前 ImageView 存在另一个图片请求任务时,
     * 则把他取消掉并返回 true,否则返回 false;
     * @param url
     * @param imageView
     * @return
     */
    public boolean cancelPotentialWork(String url, ImageView imageView) {
        BitmapWorkerTask task = getBitmapWorkTask(imageView);
        if (task != null ) {
            String imageUrl = task.imageUrl;
            if (imageUrl == null || !url.equals(imageUrl)) {
                task.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取传入 ImageView 所对应的 BitmapWorkTask.
     * @param imageView
     * @return
     */
    public BitmapWorkerTask getBitmapWorkTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                return ((AsyncDrawable) drawable).getBitmapWorkTask();
            }
        }
        return null;
    }

    /**
     * 自定义的 Drawable,让这个 Drawable 持有 BitmapWorkerTask 弱引用.
     */
    class AsyncDrawable extends BitmapDrawable {
        private WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources resources, Bitmap bitmap, BitmapWorkerTask task) {
            super(resources, bitmap);
            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(task);
        }

        public BitmapWorkerTask getBitmapWorkTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    /**
     * 异步下载任务.
     */
    class BitmapWorkerTask extends AsyncTask<String, Void, BitmapDrawable> {

        private String imageUrl;

        private WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
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
            ImageView imageView = getAttachedImageView();
            if (imageView != null && bitmapDrawable != null) {
                imageView.setImageDrawable(bitmapDrawable);
            }
        }

        /**
         * 获取当前 BitmapWorkTask 所关联的 ImageVIew.
         * @return
         */
        public ImageView getAttachedImageView() {
            ImageView imageView = imageViewReference.get();
            BitmapWorkerTask task = getBitmapWorkTask(imageView);
            if (this == task) {
                return imageView;
            }
            return null;
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
