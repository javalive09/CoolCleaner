package com.peter.coolcleaner;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class CoolCleanerService extends AccessibilityService {

	private static final String TAG = CoolCleanerService.class.getSimpleName();
	
	public static final String ACTION_FINISH_SETTING_ACCESS_ACT = "action_finish_setting_access_act";
	
	public static final String SHELF_CLASSNAME = "com.peter.coolcleaner.Main";
	
	public static boolean authorized;
	
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();
        Log.d(TAG, "事件---->" + event);
        if(canPass()) { 
	        if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
	            clickForceStop(event);
	        }
        }
    }
    
    private boolean canPass() {
    	ActivityManager mActivityManager = ((ActivityManager) getSystemService(ACTIVITY_SERVICE));
    	@SuppressWarnings("deprecation")
		List<RunningTaskInfo> tasksInfo = mActivityManager.getRunningTasks(1);
	    ComponentName name =  tasksInfo.get(0).baseActivity;
	    if(SHELF_CLASSNAME.equals(name.getClassName())) {
	    	return true;
	    }
	    return false;
    }

    private void clickForceStop(AccessibilityEvent event) {
    	String detailClassName = "com.android.settings.applications.InstalledAppDetailsTop";
    	String detailClassName_cuizi = "com.android.settings.applications.InstalledAppDetailsActivity";
    	String stopOkDialogClassName = "android.app.AlertDialog";
    	String subSettingClassName = "com.android.settings.SubSettings";
    	String subSettingClassName_meizu = "com.android.settings.Settings$AccessibilitySettingsActivity";
    	AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
		
    	if(nodeInfo == null) {
			Log.w(TAG, "rootWindow为空");
			return;
		}
    	
    	if(event.getClassName().equals(detailClassName) ||
    			event.getClassName().equals(detailClassName_cuizi)) {
    		List<AccessibilityNodeInfo> list = null;
    		if(Main.forceStop) {
    			list = nodeInfo.findAccessibilityNodeInfosByText("强行停止");
				if(list.size() == 0) {
					list = nodeInfo.findAccessibilityNodeInfosByText("强制停止");
				}
				if(list.size() == 0) {
					list = nodeInfo.findAccessibilityNodeInfosByText("结束程序");
				}
    		}
			
    		if(list != null) {
				for(AccessibilityNodeInfo n : list) {
					n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
				}
    		}
			
		}else if(event.getClassName().equals(stopOkDialogClassName)) {
			if(Main.forceStop) {
				List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("确定");
				for(AccessibilityNodeInfo n : list) {
					n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
				}
			}
			
		}else if(event.getClassName().equals(subSettingClassName)
				||event.getClassName().equals(subSettingClassName_meizu)) {//关闭access subsetting
			if(!authorized && isAuthorized(nodeInfo)) {
				authorized = true;
				Intent intent = new Intent(ACTION_FINISH_SETTING_ACCESS_ACT);
				Uri data = Uri.parse("package:" + SHELF_CLASSNAME);
				intent.setData(data);
				sendBroadcast(intent);
			}
		}
    	
    }

    private boolean isAuthorized(AccessibilityNodeInfo nodeInfo) {
		if(android.os.Build.BRAND.equals("nubia")) {//nubia 手机适配
			List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("开启");//nubia
			if(list.size() > 0) {
				return true;
			}
			return false;
		}else {
			return true;
		}



    }
    
	@Override
	public void onInterrupt() {
		
	}

}
