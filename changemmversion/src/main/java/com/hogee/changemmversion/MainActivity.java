package com.hogee.changemmversion;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final boolean DEBUG = true;
    private static final String TAG = "CMV:MainA";

    private String INSTALL_APK_TMP_PATH = "/sdcard/changemmversion/";

    public TextView mIntroduceView;
    private Button mSmartInstall;
    private Button mInstallMM665;
    private Button mInstallMM653;
    private Button mUnInstallMM665;
    private Button mUnInstallMM653;
    private Resources mRes;

    private Context mContext;

    private TextView mTextMessage;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.introduce);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mContext = this;
        mRes = this.getResources();
        mIntroduceView = (TextView) findViewById(R.id.introduce);
        mSmartInstall = (Button) findViewById(R.id.btn_set_smart_install);
        mInstallMM665 = (Button) findViewById(R.id.btn_install_665);
        mInstallMM653 = (Button) findViewById(R.id.btn_install_653);
        mUnInstallMM665 = (Button) findViewById(R.id.btn_uninstall_665);
        mUnInstallMM653 = (Button) findViewById(R.id.btn_uninstall_653);

        mSmartInstall.setOnClickListener(this);
        mInstallMM665.setOnClickListener(this);
        mInstallMM653.setOnClickListener(this);
        mUnInstallMM665.setOnClickListener(this);
        mUnInstallMM653.setOnClickListener(this);

        //检测读写权限
        PermisionUtils.verifyStoragePermissions(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_set_smart_install:
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                break;
            case R.id.btn_install_665:
                //Toast.makeText(this, "安装mm665", Toast.LENGTH_LONG).show();
                String[] mm_665 = mRes.getStringArray(R.array.mm_665_file_name);
                if (mm_665.length > 0){
                    CopyInstallApkAsyncTask copyInstallApkAsyncTask = new CopyInstallApkAsyncTask();
                    copyInstallApkAsyncTask.execute(mm_665[0]);
                } else {
                    Toast.makeText(this, "配置错误：没有配置mm665", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_install_653:
                //Toast.makeText(this, "安装mm653", Toast.LENGTH_LONG).show();
                String[] mm_653 = mRes.getStringArray(R.array.mm_653_file_name);
                if (mm_653.length > 0){
                    CopyInstallApkAsyncTask copyInstallApkAsyncTask = new CopyInstallApkAsyncTask();
                    copyInstallApkAsyncTask.execute(mm_653[0]);
                } else {
                    Toast.makeText(this, "配置错误：没有配置mm653", Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.btn_uninstall_665:
                Toast.makeText(this, "智能卸载mm665", Toast.LENGTH_LONG).show();
                UninstallApkAsyncTask uninstallApkAsyncTask = new UninstallApkAsyncTask();
                uninstallApkAsyncTask.execute("com.tencent.mm");
                break;
            case R.id.btn_uninstall_653:
                Toast.makeText(this, "智能卸载mm653", Toast.LENGTH_LONG).show();
                UninstallApkAsyncTask uninstallApkAsyncTask653 = new UninstallApkAsyncTask();
                uninstallApkAsyncTask653.execute("com.tencent.mm");
                break;

            default:
                break;
        }

    }

    private void smartUninstall(String packageName){
        Log.d(TAG, "smart uninstall apk:"+packageName);
        Uri uri = Uri.parse("package:"+packageName);
        if (uri != null) {
            Intent localIntent = new Intent(Intent.ACTION_DELETE, uri);
            localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(localIntent);
        } else {
            Toast.makeText(this, "卸载解析失败", Toast.LENGTH_LONG).show();
        }
    }

    private class UninstallApkAsyncTask extends AsyncTask<String, Void, Void> {
        private String packageName = "com.tencent.mm";
        private int statusFlag = 1;

        @Override
        protected Void doInBackground(String... params) {
            packageName = params[0];
            Log.d(TAG, "apk package name :"+packageName);
            if (packageName.equals("")) {
                Log.e(TAG, "package name is empty, return");
                statusFlag = -1;
                return null;
            }

            PackageInfo packageInfo;
            try {
                packageInfo = mContext.getPackageManager().getPackageInfo(packageName, 0);
            } catch (Exception e) {
                packageInfo = null;
                Log.e(TAG, "package manager info get error:", e);
                e.printStackTrace();
            }
            if(packageInfo == null){
                Log.e(TAG, packageName + " APK not exist in this device, cannot uninstall");
                statusFlag = -2;
                return null;
            }
            statusFlag = 1;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (statusFlag == 1){
                Log.d(TAG, "began to uninstall "+packageName);
                smartUninstall(packageName);
            } else if (statusFlag == -2){
                Toast.makeText(mContext, packageName + ":此应用没有安装过", Toast.LENGTH_LONG).show();
            }
        }
    }

    private class CopyInstallApkAsyncTask extends AsyncTask<String, Void, Void>{

        private String assetsFolder = "";
        private String apkFileName = "mm.apk";
        private String apkPath = "";

        @Override
        protected Void doInBackground(String... params) {
            apkFileName = params[0];
            Log.d(TAG, "apk file name :"+apkFileName);
            if (apkFileName.equals("")) {
                Log.e(TAG, "apk name is empty, return");
                return null;
            }
            try {
                String fileNames[] = mContext.getAssets().list(assetsFolder);
                if (fileNames.length > 0) {
                    File file = new File(Environment.getExternalStorageDirectory(), "/changemmversion/");
                    if (!file.exists()) {
                        Log.d(TAG, INSTALL_APK_TMP_PATH+" create file dir");
                        file.mkdirs();
                    }

                    Log.d(TAG, "file path="+(file.getPath() + File.separator + apkFileName));
                    File outFile = new File(file.getPath() + File.separator + apkFileName);
                    if (!outFile.exists()) {
                        try {
                            Log.d(TAG, "will create file");
                            outFile.createNewFile();
                        } catch (IOException ioe) {
                            Log.e(TAG, "create new file error:", ioe);
                        }

                        InputStream is = mContext.getAssets().open(apkFileName);
                        FileOutputStream fos = new FileOutputStream(outFile);
                        byte[] buffer = new byte[1024];
                        int byteCount;
                        while ((byteCount = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, byteCount);
                        }
                        fos.flush();
                        is.close();
                        fos.close();
                    } else {
                        Log.d(TAG, "the file already exist");
                    }

                } else {
                    return null;
                }

            } catch (Exception e){
                e.printStackTrace();
                Log.e(TAG, "copyAssetsToDst have error:", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            smartInstall(INSTALL_APK_TMP_PATH + apkFileName);
        }

        //智能安装
        private void smartInstall(String apkPath) {
            Log.d(TAG, "smart install apk:"+apkPath);
            //Uri uri = Uri.fromFile(new File(apkPath));
            Intent localIntent = new Intent(Intent.ACTION_VIEW);
            //localIntent.setDataAndType(uri, "application/vnd.android.package-archive");
            //判断是否是AndroidN以及更高的版本
            if (Build.VERSION.SDK_INT >= 24) {//Build.VERSION_CODES.N
                localIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri contentUri = FileProvider.getUriForFile(mContext,
                        "com.tencent.mm.fileProvider", new File(apkPath));
                localIntent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            } else {
                localIntent.setDataAndType(Uri.fromFile(new File(apkPath)), "application/vnd.android.package-archive");
                localIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            Log.d(TAG, "began to start package installer");
            mContext.startActivity(localIntent);
        }
    }

    /**
     * 判断AccessibilityService服务是否已经启动, NOUSE now
     * @param context
     * @param name
     * @return
     */
    public static boolean isStartAccessibilityService(Context context, String name){
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> serviceInfos = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo info : serviceInfos) {
            String id = info.getId();
            Log.d(TAG, "all -->" + id);
            if (id.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }

    };

}
