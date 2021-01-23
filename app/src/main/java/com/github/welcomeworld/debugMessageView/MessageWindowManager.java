package com.github.welcomeworld.debugMessageView;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MessageWindowManager {

    private static MessageWindowManager instance;
    private static Handler uiHandler;

    public static MessageWindowManager getInstance() {
        if (instance == null) {
            instance = new MessageWindowManager();
            uiHandler = new Handler(Looper.getMainLooper());
        }
        return instance;
    }

    WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();

    public WindowManager getWindowManager() {
        return windowManager;
    }

    WindowManager windowManager;
    View playView;
    private TextView closeView;
    private WindowManager.LayoutParams closeWindowLayoutParams = new WindowManager.LayoutParams();
    private View layoutView;
    private RecyclerView recyclerView;
    MessageAdapter messageAdapter;
    private int screenHeight;
    private int screenWidth;
    private float mTouchStartX;
    private float mTouchStartY;
    private boolean move = false;
    private boolean cancel = false;
    private boolean power = false;
    private float x;
    private float y;
    private float startX;
    private float starty;
    private Context mContext;
    private int statusHeight = 0;
    private static boolean bindWindow = false;

    public static boolean isBindWindow() {
        return bindWindow;
    }

    @MainThread
    public void initWindow(Context context) {
        this.mContext = context;
        if (!(Build.VERSION.SDK_INT < 23 || android.provider.Settings.canDrawOverlays(mContext))) {
            Toast.makeText(mContext,"Error:can not show debugView without popupWindow permission",Toast.LENGTH_LONG).show();
            return;
        }
        if (windowManager != null && layoutParams != null) {
            return;
        }
        if (windowManager == null) {
            windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }
        if (layoutParams == null) {
            layoutParams = new WindowManager.LayoutParams();
        }
        screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        if (playView == null) {
            playView = LayoutInflater.from(mContext).inflate(R.layout.debug_message_view_window_overlay, null, false);
        }
        if (layoutView == null) {
            layoutView = playView.findViewById(R.id.drag_view);
        }
        if(recyclerView == null){
            recyclerView = playView.findViewById(R.id.message_view);
            recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
            messageAdapter = new MessageAdapter();
            recyclerView.setAdapter(messageAdapter);
        }
        closeView = (TextView) LayoutInflater.from(mContext).inflate(R.layout.debug_message_view_window_close_view, null, false);
        closeView.setVisibility(View.GONE);
        initLayoutParams();
        initEvent();
        statusHeight = getStatusBarHeight(context);
        try {
            windowManager.addView(playView, layoutParams);
            bindWindow = true;
            windowManager.addView(closeView, closeWindowLayoutParams);
        } catch (Exception e) {
            //ignore
        }
    }

    public static int getStatusBarHeight(Context context) {
        try {
            int result = 0;
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = context.getResources().getDimensionPixelSize(resourceId);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void initLayoutParams() {

        //总是出现在应用程序窗口之上。
        if (Build.VERSION.SDK_INT >= 26) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            closeWindowLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            closeWindowLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        // FLAG_NOT_TOUCH_MODAL不阻塞事件传递到后面的窗口
        // FLAG_NOT_FOCUSABLE 悬浮窗口较小时，后面的应用图标由不可长按变为可长按,不设置这个flag的话，home页的划屏会有问题
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        closeWindowLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

        //悬浮窗默认显示的位置
        layoutParams.gravity = Gravity.START | Gravity.TOP;
        closeWindowLayoutParams.gravity = Gravity.CENTER | Gravity.BOTTOM;
        closeWindowLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        closeWindowLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        closeWindowLayoutParams.format = PixelFormat.TRANSPARENT;

        //指定位置
        layoutParams.x = screenWidth - layoutView.getLayoutParams().width;
        layoutParams.y = screenHeight / 3 - layoutView.getLayoutParams().height;
        //悬浮窗的宽高
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.format = PixelFormat.TRANSPARENT;
    }

    private void initEvent() {
        layoutView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (v.getId() == R.id.drag_view) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
//                    flingView.setBackgroundResource(R.color.transparent);
                            //根据上次手指离开的位置与此次点击的位置进行初始位置微调
                            mTouchStartX = (event.getRawX() - layoutParams.x);
                            mTouchStartY = (event.getRawY() - layoutParams.y);
                            startX = event.getX();
                            starty = event.getY();
                            startTime = System.currentTimeMillis();
                            cancel = false;
                            power = false;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            // 获取相对屏幕的坐标，以屏幕左上角为原点
                            if ((Math.abs(event.getX() - startX) > 25 || Math.abs(event.getY() - starty) > 25)) {
                                move = true;
                            }
                            closeView.setVisibility(View.VISIBLE);
                            x = event.getRawX();
                            y = event.getRawY();
                            int[] cv = new int[2];
                            closeView.getLocationOnScreen(cv);
                            int[] wv = new int[2];
                            closeView.getLocationInWindow(wv);
                            if ((layoutParams.y + layoutView.getLayoutParams().height) > cv[1] - statusHeight) {
                                closeView.setText("松开关闭");
                            } else {
                                closeView.setText("关闭");
                            }
                            updateViewPosition();
                            break;

                        case MotionEvent.ACTION_UP:
                            mTouchStartX = 0;
                            mTouchStartY = 0;
                            int[] uv = new int[2];
                            closeView.getLocationOnScreen(uv);
                            if ((layoutParams.y + layoutView.getLayoutParams().height) > uv[1] - statusHeight) {
                                cancelWindow();
                                cancel = true;
                            }
                            closeView.setVisibility(View.GONE);
                            move = false;
                            break;
                    }
                    return true;
                }
                return false;
            }
        });
    }

    boolean isDouble = false;
    private long lastTouch;
    private long startTime;

    private void updateViewPosition() {
        int realX = (x - mTouchStartX) > screenWidth - layoutView.getLayoutParams().width / 2 ? screenWidth - layoutView.getLayoutParams().width / 2 : (int) (x - mTouchStartX);
        int realY = (y - mTouchStartY) > screenHeight - layoutView.getLayoutParams().height / 2 ? screenHeight - layoutView.getLayoutParams().height / 2 : (int) (y - mTouchStartY);
        realX = realX < 0 ? 0 : realX;
        realY = realY < 0 ? 0 : realY;
        layoutParams.x = realX;
        layoutParams.y = realY;
        if (windowManager != null) {
            windowManager.updateViewLayout(playView, layoutParams);
        }
    }

    public static long cancelWindowTime = 0;

    public void cancelWindow() {
        cancelWindowTime = System.currentTimeMillis();
        if (windowManager != null) {
            try {
                if (playView.isAttachedToWindow()) {
                    windowManager.removeView(playView);
                }
            } catch (Exception e) {
                //ignore
            }
            try {
                if (closeView.isAttachedToWindow()) {
                    windowManager.removeView(closeView);
                }
            } catch (Exception e) {
                //ignore
            }
            windowManager = null;
        }
        bindWindow = false;
    }

    public void addMessage(String message){
        if(Looper.myLooper()!=Looper.getMainLooper()){
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(messageAdapter!=null){
                        messageAdapter.getMessages().add(message);
                        messageAdapter.notifyItemInserted(messageAdapter.getMessages().size()-1);
                        recyclerView.scrollToPosition(messageAdapter.getMessages().size()-1);
                    }
                }
            });
        }else {
            if(messageAdapter!=null){
                messageAdapter.getMessages().add(message);
                messageAdapter.notifyItemInserted(messageAdapter.getMessages().size()-1);
                recyclerView.scrollToPosition(messageAdapter.getMessages().size()-1);
            }
        }
    }


}

