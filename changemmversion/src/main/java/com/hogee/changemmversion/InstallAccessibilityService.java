package com.hogee.changemmversion;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kevin on 2018/4/6 0006.
 */

public class InstallAccessibilityService extends AccessibilityService {

    public static final boolean DEBUG = true;
    private static final String TAG = "CMV:IAService";

    private Map<Integer, Boolean> handleMap = new HashMap<Integer, Boolean>();

    public InstallAccessibilityService() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "oncreate running in");
        super.onCreate();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d(TAG, "onAccessibilityEvent running in");
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo != null) {
            int eventType = event.getEventType();
            if (DEBUG) Log.d(TAG, "onAccessibilityEvent eventType="+eventType);
            if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                    eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (DEBUG) Log.d(TAG, "onAccessibilityEvent 3");
                if (handleMap.get(event.getWindowId()) == null) {
                    if (DEBUG) Log.d(TAG, "onAccessibilityEvent 4");
                    boolean handled = iterateNodesAndHandle(nodeInfo);
                    if (handled) {
                        handleMap.put(event.getWindowId(), true);
                    }
                }
            }
        }
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        Log.d(TAG, "onServiceConnected running in");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.packageNames = new String[]{"com.google.android.packageinstaller",
                "com.android.packageinstaller"};
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 5000;
        setServiceInfo(info);
        super.onServiceConnected();
        AccessibilityServiceInfo checkIfo = getServiceInfo();
        Log.d(TAG, checkIfo.packageNames[0]);
    }

    private boolean isNeedClickView(String nodeCotent){
        String[] needCheck = {"下一步", "安装", "打开", "确定",
                               "NEXT", "INSTALL", "OPEN",
                                "OK", "删除", "DELETE", "Delete"};
        for (String checkIntem : needCheck){
            if (checkIntem.equals(nodeCotent)){
                return true;
            }
        }
        return false;
    }

    /**
     * 遍历节点，模拟点击安装按钮
     * @param nodeInfo
     * @return
     */
    private boolean iterateNodesAndHandle(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo != null) {
            int childCount = nodeInfo.getChildCount();
            //if (DEBUG) Log.d(TAG, "onAccessibilityEvent 6");
            if ("android.widget.Button".equals(nodeInfo.getClassName())) {
                //if (DEBUG) Log.d(TAG, "onAccessibilityEvent 7");
                String nodeCotent = nodeInfo.getText().toString();
                Log.d(TAG, "content is: " + nodeCotent);
                if (isNeedClickView(nodeCotent)) {
                    nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
            //遇到ScrollView的时候模拟滑动一下
            else if ("android.widget.ScrollView".equals(nodeInfo.getClassName())) {
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            }
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(i);
                if (iterateNodesAndHandle(childNodeInfo)) {
                    return true;
                }
            }
        }
        return false;
    }
}
