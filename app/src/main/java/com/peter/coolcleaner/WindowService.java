package com.peter.coolcleaner;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

public class WindowService extends Service {

	public Board board;
	
	public static Main main;

	@Override
	public IBinder onBind(Intent intent) {
		return new MyBinder();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		WindowManager wm = (WindowManager) getApplicationContext()
				.getSystemService(Context.WINDOW_SERVICE);
		WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
		wmParams.type = WindowManager.LayoutParams.TYPE_TOAST;
		wmParams.format = PixelFormat.RGBA_8888;
		wmParams.flags = 40;
		wmParams.width = LayoutParams.MATCH_PARENT;
		wmParams.height = LayoutParams.MATCH_PARENT;
		board = new Board(main, null);
		wm.addView(board, wmParams);
		PropertyValuesHolder pvhAlpha = PropertyValuesHolder.ofFloat("alpha", .0f, 1.0f);
        ObjectAnimator bAnim = ObjectAnimator.ofPropertyValuesHolder(board,
                pvhAlpha).setDuration(1000);
        bAnim.start();
	}

	@Override
	public boolean onUnbind(Intent intent) {
		WindowManager wm = (WindowManager) getApplicationContext()
				.getSystemService(Context.WINDOW_SERVICE);
		wm.removeView(board);
		return super.onUnbind(intent);
	}

	public class MyBinder extends Binder {

		public WindowService getService() {
			return WindowService.this;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

}
