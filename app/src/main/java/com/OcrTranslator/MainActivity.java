package com.OcrTranslator;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.OcrTranslator.gson.TransResult;
import com.OcrTranslator.service.ScreenshotService;
import com.OcrTranslator.service.TransService;
import com.OcrTranslator.utils.HttpUtil;
import com.OcrTranslator.utils.Utility;
import com.google.android.material.navigation.NavigationView;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private DrawerLayout drawerLayout;

    private EditText editText;

    private TextView textView;

    private String ocrText;

    private Spinner from;

    private Spinner to;

    private MediaProjectionManager mMediaProjectionManager = null;

    private int width, height, dpi;

    private ScreenshotService.ScreenshotBinder screenshotBinder;

    //声明一个操作常量字符串
//    public static final String ACTION_SERVICE_NEED = "action.ServiceNeed";
    //声明一个内部广播实例
//    public ServiceNeedBroadcastReceiver broadcastReceiver;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            screenshotBinder = (ScreenshotService.ScreenshotBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        自定义标题栏
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        }

//        滑动菜单
        drawerLayout = findViewById(R.id.drawLayout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener(){
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.settings:
                        Intent intent =new Intent(MainActivity.this,TransImageActivity.class);
                        startActivity(intent);
                        break;
                    default:
                }
                return false;
            }
        });

//        翻译语言切换
        from = findViewById(R.id.language1);
        to = findViewById(R.id.language2);
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(
                this,R.array.languageList1,R.layout.spinnerlayout);
        adapter1.setDropDownViewResource(R.layout.spinnerlayout);
        from.setAdapter(adapter1);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                this,R.array.languageList2,R.layout.spinnerlayout);
        adapter2.setDropDownViewResource(R.layout.spinnerlayout);
        to.setAdapter(adapter2);
        ImageView switchover = findViewById(R.id.switchover);
        switchover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (from.getSelectedItemId() != 0) {
//                    switchover.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_switchover));
                    int f = (int)from.getSelectedItemId();
                    int t = (int)to.getSelectedItemId();
                    from.setSelection(t+1);
                    to.setSelection(f-1);
                } else {
                    Toast.makeText(getApplicationContext(),"目标语言不能为自动",Toast.LENGTH_SHORT).show();
                }
            }
        });

//        翻译
        editText = findViewById(R.id.editText);
        Button trans = findViewById(R.id.translate);
        trans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (editText.getText() != null) {
                    List<String> inputText = new ArrayList<>();
                    String language1 = from.getSelectedItem().toString();
                    String language2 = to.getSelectedItem().toString();
                    inputText.add(editText.getText().toString());
                    trans(inputText,language1,language2);
                }
            }
        });

//        复制翻译结果
        textView = findViewById(R.id.transText);
        Button copy = findViewById(R.id.copy);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //获取剪贴板管理器：
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                // 创建普通字符型ClipData
                ClipData mClipData = ClipData.newPlainText("Label", textView.getText());
                // 将ClipData内容放到系统剪贴板里。
                cm.setPrimaryClip(mClipData);
            }
        });

//        获取从相册返回的ocr结果并写进文本框
        ActivityResultLauncher<Intent> requestAlbumLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        assert result.getData() != null;
                        ocrText = result.getData().getStringExtra("ocrText");
                        if (ocrText != null){
                            editText = findViewById(R.id.editText);
                            editText.setText(ocrText);
                        }
                    }
                }
        });

//        打开相册
        Button openAlbum = findViewById(R.id.openAlbum);
        openAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =new Intent(MainActivity.this, OpenAlbumActivity.class);
                intent.putExtra("from", from.getSelectedItem().toString());
                intent.putExtra("to", to.getSelectedItem().toString());
                requestAlbumLauncher.launch(intent);
            }
        });

//        悬浮窗
        Button floatView = findViewById(R.id.floatView);
        floatView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //检查是否已经授予权限
                if (!Settings.canDrawOverlays(MainActivity.this)) {
                    //若未授权则请求权限
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 0);
                    FloatBallView.getInstance(MainActivity.this).createFloatView();
                }
                FloatBallView.getInstance(MainActivity.this).createFloatView();
                mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                Intent intent = mMediaProjectionManager.createScreenCaptureIntent();
                screenshotLauncher.launch(intent);
            };
        });

        FloatBallView.getInstance(MainActivity.this).onFloatViewClick(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startCapture();
                screenshotBinder.Screenshot();
//                Glide.with(MainActivity.this).load(uri).into(imageView);
//                unbindService(connection);
//                Intent stopIntent = new Intent(MainActivity.this,ScreenshotService.class);
//                stopService(stopIntent);
            }
        });

        /**
         * 注册广播实例（在初始化的时候）
         */
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(ACTION_SERVICE_NEED);
//        broadcastReceiver = new ServiceNeedBroadcastReceiver();
//        registerReceiver(broadcastReceiver, filter);

//        Button clearCache = findViewById(R.id.clearCache);
//        clearCache.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                OcrService ocrService = new OcrService();
//                ocrService.clearCache();
//                Log.d("clearCache","清除缓存");
//            }
//        });
    }
    public static Bitmap capture(Activity activity) {
        activity.getWindow().getDecorView().setDrawingCacheEnabled(true);
        Bitmap bmp = activity.getWindow().getDecorView().getDrawingCache();
        return bmp;
    }

//    获取相机返回的ocr结果并写进文本输入框
    ActivityResultLauncher<Intent> requestCameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    assert result.getData() != null;
                    ocrText = result.getData().getStringExtra("ocrText");
                    if (ocrText != null){
                        editText = findViewById(R.id.editText);
                        editText.setText(ocrText);
                    }
                }
            }
        });

    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
//            打开相机
            case R.id.camera:
                Intent intent =new Intent(MainActivity.this, CameraActivity.class);
                requestCameraLauncher.launch(intent);
                break;
//            打开滑动菜单
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
            default:
        }
        return true;
    }

    private void trans(List<String> transText, String from, String to) {
        TransService transService = new TransService();
        String address = transService.TransResult(transText, from, to);
        final String[] result = {""};
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                assert response.body() != null;
                List<TransResult> transResults = Utility.handleTransResultResponse(response.body().string());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for (int i = 0; i < transResults.size(); i++){
                                result[0] = result[0] + transResults.get(i).trans;
                            }
                            textView.setText(result[0]);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });
    }

    ActivityResultLauncher<Intent> screenshotLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_CANCELED) {
                Log.e(TAG, "User cancel");
            } else {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        width = getWindowManager().getCurrentWindowMetrics().getBounds().width();
                        height = getWindowManager().getCurrentWindowMetrics().getBounds().height();
                        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                        dpi = displayMetrics.densityDpi;
                        Log.e(TAG, "width: " + width + ",height:" + height + ",sdk >= 30");
                    } else {
                        //获取减去系统栏的屏幕的高度和宽度
                        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                        width = displayMetrics.widthPixels;
                        height = displayMetrics.heightPixels;
                        dpi = displayMetrics.densityDpi;
                        Log.e(TAG, "width: " + width + ",height:" + height + ",sdk <= 30");
                    }
                } catch (Exception e){
                    Log.e(TAG, "MediaProjection error");
                }
                Intent service = new Intent(MainActivity.this, ScreenshotService.class);
                service.putExtra("code", result.getResultCode());
                service.putExtra("data", result.getData());
                service.putExtra("from", from.getSelectedItem().toString());
                service.putExtra("to", to.getSelectedItem().toString());
                service.putExtra("dpi",dpi);
                service.putExtra("width",width);
                service.putExtra("height",height);
                bindService(service, connection, BIND_AUTO_CREATE);
//                startService(service);
                startForegroundService(service);
            }
        }
    });

    /**
     * 定义广播接收器，用于执行Service服务的需求（内部类）
     */
//    private class ServiceNeedBroadcastReceiver extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            Log.d(TAG, "接收截图");
//            //这里是要在Activity活动里执行的代码
//            uri = intent.getParcelableExtra("uri");
//        }
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }
}