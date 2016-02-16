package com.peter.coolcleaner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.peter.coolcleaner.Board.Head;
import com.peter.coolcleaner.WindowService.MyBinder;
import com.peter.coolcleaner.factory.FallingParticleFactory;

import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class Main extends Activity {

	public static boolean forceStop;
	
	public static boolean clearAll;
	
	private String forecStopPackageName;
	
	private BroadcastReceiver mReceiver;
	
	public HashMap<AppInfo, View> mforceStopMap;
	
	WindowService mService;
	
	ServiceConnection conn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ImageView iv = new ImageView(this);
		iv.setBackgroundResource(R.drawable.bg);
		setContentView(iv);
		refreshData();
	}

	private void refreshData() {
		WindowService.main = this;
		mforceStopMap = getRunningAppInfos();
		forceStop = false;
		if(CoolCleanerService.authorized) {
			bindService();
		}
		new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

			@Override
			public void run() {
				if (!CoolCleanerService.authorized) {
					//为了接收关闭access setting 的广播
					AppInfo info = new AppInfo();
					info.packageName = CoolCleanerService.SHELF_CLASSNAME;
					registReceiver(info);

					//启动access setting 界面
					Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
					startActivity(intent);
				}
//				String channel = getApplicationMetaValue("UMENG_CHANNEL");
//				Toast.makeText(Main.this, "channel:" + channel, Toast.LENGTH_LONG).show();
			}
		}, 1000);
	}

	private String getApplicationMetaValue(String name) {
		String value= "";
		try {
			ApplicationInfo appInfo =getPackageManager()
					.getApplicationInfo(getPackageName(),
							PackageManager.GET_META_DATA);
			value = appInfo.metaData.getString(name);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return value;
	}

	private void bindService() {
		if(mService == null && conn == null) {
			Intent intent = new Intent(Main.this, WindowService.class);
			conn = new ServiceConnection() {

				@Override
				public void onServiceConnected(ComponentName name, IBinder service) {
					MyBinder binder = (MyBinder)service;
					mService = binder.getService();
					mService.board.startAnimation();
				}

				@Override
				public void onServiceDisconnected(ComponentName name) {
				}
				
			};
			bindService(intent, conn, Context.BIND_AUTO_CREATE);
		}
	}

	public void clearAll() {
		if(mforceStopMap.size() > 0) {
			Iterator<Map.Entry<AppInfo,View>> iterator = mforceStopMap.entrySet().iterator();
			while(iterator.hasNext()) {
				Map.Entry<AppInfo,View> entry = iterator.next();
				final AppInfo info = entry.getKey();
				Head nv = (Head) entry.getValue();
				if(!info.isLock) {
					Main.forceStop = true;

					final View head = nv;
					nv.killed = true;
					Main.forceStop = true;

					//explosion
					ExplosionField explosionField = new ExplosionField(Main.this, new FallingParticleFactory());
					explosionField.explode(nv, new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							super.onAnimationEnd(animation);
							mforceStopMap.remove(info);
							Board b = (Board) head.getParent();
							b.removeView(head);
							showForceStopView(info);
						}
					});

					return;
				}
			}
		}
		clearAll = false;
	}

	private Handler mHandler = new Handler(Looper.getMainLooper());

	Runnable clearNext = new Runnable() {
		@Override
		public void run() {
			clearAll();
		}
	};

	public void showForceStopView(AppInfo info) {
		Intent intent = new Intent();
		intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
		Uri uri = Uri.fromParts("package", info.packageName, null);
		intent.setData(uri);
		registReceiver(info);
		startActivity(intent);
	}
	
	private class MyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Uri data = intent.getData();
			if (data != null) {
				String str = data.getSchemeSpecificPart();
				if(!TextUtils.isEmpty(str)) {
					if (str.equals(forecStopPackageName)) {
	
						String action = intent.getAction();
						if (Intent.ACTION_PACKAGE_RESTARTED.equals(action)) {
							finishSetting("forceStop");
							unRegisterMyReceiver();
							
							if(clearAll) {
								mHandler.postDelayed(clearNext, 600);
							}
						
						} else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
							
						} else if (action.equals(CoolCleanerService.ACTION_FINISH_SETTING_ACCESS_ACT)) {
							finishSetting("");
							unRegisterMyReceiver();
							bindService();
						}
					}
				}
			}
		}

	}
	
	
	
	@Override
	public void finish() {
		if(mService != null) {
			if(mService.board != null) {
				mService.board.setVisibility(View.GONE);
			}
		}
		super.finish();
		overridePendingTransition(0, R.anim.zoom_exit);
	}

	private void finishSetting(String invoke) {
		Intent in = new Intent();
		in.putExtra("whoInvoke", invoke);
		in.setClass(Main.this, Main.class);
		in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);// 确保finish掉setting
		in.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);// 确保MainActivity不被finish掉
		startActivity(in);
	}
	
	private void registReceiver(AppInfo info) {
		forecStopPackageName = info.packageName;
		unRegisterMyReceiver();
		mReceiver = new MyReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_PACKAGE_RESTARTED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addAction(CoolCleanerService.ACTION_FINISH_SETTING_ACCESS_ACT);
		filter.addDataScheme("package");
		registerReceiver(mReceiver, filter);
	}
	
	private void unRegisterMyReceiver() {
		if(mReceiver != null) {
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(conn != null) {
			unbindService(conn);
		}
		stopService(new Intent(this, WindowService.class));
	}

	static class AppInfo {
		public String appName;
		public String packageName;
		public BitmapDrawable appIcon;
		public Boolean isLock;
	}

	public HashMap<AppInfo, View> getRunningAppInfos() {
		ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		PackageManager pm = getPackageManager();
		List<ApplicationInfo> appList = pm
				.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
		// 正在运行的进程
		List<RunningAppProcessInfo> runningAppProcessInfos = mActivityManager
				.getRunningAppProcesses();
		// 正在运行的应用

		HashMap<AppInfo, View> map = new HashMap<AppInfo, View>(runningAppProcessInfos.size());
		for (RunningAppProcessInfo runningAppInfo : runningAppProcessInfos) {// 遍历正在运行的程序

			ArrayList<ApplicationInfo> infos = getAppInfo(
					runningAppInfo.pkgList, appList);// 获取正在运行的程序信息

			for (ApplicationInfo applicationInfo : infos) {
				if (applicationInfo != null 
						&& !isSystemApp(applicationInfo)
						&& !getPackageName().equals(applicationInfo.packageName)) {// 非系统程序
					
					AppInfo info = new AppInfo();
					info.packageName = applicationInfo.packageName;
					BitmapDrawable bitmapDrawable = (BitmapDrawable) applicationInfo
							.loadIcon(pm);
					info.appName = applicationInfo.loadLabel(pm).toString();
					info.isLock = getLock(info.packageName);
					info.appIcon = bitmapDrawable;
					if (!containInfo(map, info)) {
						map.put(info, null);
					}
				}
			}
		}
		return map;
	}

	private boolean containInfo(HashMap<AppInfo, View> map, AppInfo info) {
		for (AppInfo af : map.keySet()) {
			if (af.packageName.equals(info.packageName)) {
				return true;
			}
		}
		return false;
	}
	
	private void savelock(String packageName, boolean lock) {
		getSharedPreferences("lock", MODE_PRIVATE).edit().putBoolean(packageName, lock).commit();
	}
	
	private boolean getLock(String packageName) {
		return getSharedPreferences("lock", MODE_PRIVATE).getBoolean(packageName, false);
	}

	private boolean isSystemApp(ApplicationInfo appInfo) {
		if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) {// system apps
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 获取应用信息
	 */
	private ArrayList<ApplicationInfo> getAppInfo(String[] pkgList,
			List<ApplicationInfo> appList) {
		if (pkgList == null) {
			return null;
		}

		ArrayList<ApplicationInfo> infos = new ArrayList<ApplicationInfo>(
				pkgList.length);

		for (String pkg : pkgList) {
			for (ApplicationInfo appinfo : appList) {
				if (pkg.equals(appinfo.packageName)) {
					infos.add(appinfo);
					break;
				}
			}
		}
		return infos;
	}

	public static class MyOnGestureListener extends SimpleOnGestureListener{
		
		View mView;
		Main mContext;
		
		public MyOnGestureListener(Main main) {
			mContext = main;
		}
		
		public void setCurrentView(View view) {
			mView = view;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if(mView instanceof Head) {
				Log.i("peter", "onDoubleTap");
				AppInfo info = (AppInfo) mView.getTag();
				Head h = (Head) mView;
				info.isLock = !info.isLock;
				mContext.savelock(info.packageName, info.isLock);
				if(info.isLock) {
					h.lock.setVisibility(View.VISIBLE);
				}else {
					h.lock.setVisibility(View.GONE);
				}
			}else if(mView instanceof Board){
				//clear all
				clearAll = true;
				mContext.clearAll();
			}
			return true;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if(mView instanceof Board) {
				mContext.finish();//不要杀死自己的进程，以免下次进入会弹出辅助模式的设置页
			}
			return super.onSingleTapConfirmed(e);
		}
		
	}
	
}
