package com.innup.ms_handlerthread;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *  HandlerThread 简单使用
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    /**
     * 完成标识符
     */
    private static final int COMPLETE = 200;
    /**
     * 继续标识符
     */
    private static final int CONTINUE = 100;
    /**
     * 图片地址集合
     */
    private String[] url={
            "https://img-blog.csdn.net/20160903083245762",
            "https://img-blog.csdn.net/20160903083252184",
            "https://img-blog.csdn.net/20160903083257871",
            "https://img-blog.csdn.net/20160903083257871",
            "https://img-blog.csdn.net/20160903083311972",
            "https://img-blog.csdn.net/20160903083319668",
            "https://img-blog.csdn.net/20160903083326871"
    };

    /**
     * 显示图片的控件
     */
    private ImageView imageView;

    /**
     * 创建的 HandlerThread;
     */
    private HandlerThread handlerThread;
    /**
     * 结合 HandlerThread 使用的 Handler。
     * 该 handler 执行在 handlerThread 指定的子线程中。
     */
    private Handler handler;
    /**
     * 用来更新界面的 handler，运行在主线程。
     */
    private Handler uiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.image_view);
        //创建 HandlerThread
        handlerThread = new HandlerThread("hlt-1");
        //开启 线程，必须在 handler 初始化前，因为handler初始化需要 handler.getLooper,如果 handlerThread 没有跑起来，就拿不到 Looper了。
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                Log.i(TAG, "handleMessage: msg = " + msg.toString());
                Log.i(TAG, "handleMessage: thread name " + Thread.currentThread().getName());
                int arg1 = msg.arg1;
                if(arg1 >= 0 && arg1 < url.length){
                    //下载图片
                    Bitmap bitmap = getNetWorkBitmap(url[arg1]);
                    if(bitmap != null){
                        Message message = new Message();
                        message.what = arg1;
                        message.obj = bitmap;
                        uiHandler.sendMessage(message);
                    }
                }
                return msg.what == COMPLETE;
            }
        });
        uiHandler = new UIUpdateHandler(this);
    }

    /**
     * 根据 url 下载图片
     * @param urlString 图片 url
     * @return 显示图片的 bitmap 对象
     */
    public static Bitmap getNetWorkBitmap(String urlString) {
        URL imgUrl = null;
        Bitmap bitmap = null;
        try {
            imgUrl = new URL(urlString);
            // 使用HttpURLConnection打开连接
            HttpURLConnection urlConn = (HttpURLConnection) imgUrl
                    .openConnection();
            urlConn.setDoInput(true);
            urlConn.connect();
            // 将得到的数据转化成InputStream
            InputStream is = urlConn.getInputStream();
            // 将InputStream转换成Bitmap
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (MalformedURLException e) {
            System.out.println("[getNetWorkBitmap->]MalformedURLException");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("[getNetWorkBitmap->]IOException");
            e.printStackTrace();
        }
        return bitmap;
    }

    public void down(View view) {
        //每隔一秒去通知 子线程下载图片
        for (int i = 0; i < url.length; i++){
            Message message = new Message();
            if(i == url.length - 1){
                message.what = COMPLETE;
            }else{
                message.what = CONTINUE;
            }
            message.arg1 = i;//告诉子线程要下载图片的 url
            handler.sendMessageDelayed(message, 1000);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(handler != null && handlerThread != null){
            handler.removeCallbacks(handlerThread);
        }
        handlerThread = null;
        handler = null;
        uiHandler = null;
    }

    public static class UIUpdateHandler extends Handler{
        WeakReference<MainActivity> weakReference;

        UIUpdateHandler(MainActivity activity){
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Log.i(TAG, "handleMessage: 次数：" + msg.what);
            Bitmap bitmap = (Bitmap) msg.obj;
            weakReference.get().imageView.setImageBitmap(bitmap);
        }
    }
}
