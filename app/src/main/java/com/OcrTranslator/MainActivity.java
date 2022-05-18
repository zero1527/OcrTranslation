package com.OcrTranslator;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.OcrTranslator.gson.TransResult;
import com.OcrTranslator.service.ScreenshotService;
import com.OcrTranslator.utils.HttpUtil;
import com.OcrTranslator.utils.JsonParse;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private String imagePath;

    private EditText editText;

    private TextView textView;

    private Spinner from;

    private Spinner to;

    private MediaProjectionManager mMediaProjectionManager = null;

    private int width, height, dpi;

    private ScreenshotService.ScreenshotBinder screenshotBinder;

    private Boolean REQUEST_SCREENSHOT = false;

    //声明操作常量字符串
    public static final String ACTION_SCREENSHOT_SERVICE = "action.Screenshot.Service";

    public static final String ACTION_OCR = "action.Ocr";

    //声明内部广播实例
    public ServiceBroadcastReceiver serviceBroadcastReceiver;

    public OcrBroadcastReceiver ocrBroadcastReceiver;

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

//        翻译语言切换
        from = findViewById(R.id.from);
        to = findViewById(R.id.to);
        ImageView switchover = findViewById(R.id.switchover);
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(
                this,R.array.languageList1,R.layout.spinnerlayout);
        adapter1.setDropDownViewResource(R.layout.spinnerlayout);
        from.setAdapter(adapter1);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                this,R.array.languageList2,R.layout.spinnerlayout);
        adapter2.setDropDownViewResource(R.layout.spinnerlayout);
        to.setAdapter(adapter2);
        switchover.setOnClickListener(view -> {
            if (from.getSelectedItemId() != 0) {
                int f = (int)from.getSelectedItemId();
                int t = (int)to.getSelectedItemId();
                from.setSelection(t+1);
                to.setSelection(f-1);
            } else {
                Toast.makeText(getApplicationContext(),"目标语言不能为自动",Toast.LENGTH_SHORT).show();
            }
        });

//        翻译
        editText = findViewById(R.id.editText);
        Button trans = findViewById(R.id.translate);
        trans.setOnClickListener(view -> {
            if (editText.getText() != null) {
                String language1 = from.getSelectedItem().toString();
                String language2 = to.getSelectedItem().toString();
                String inputText = editText.getText().toString();
                translate(inputText,language1,language2);
            }
        });

//        复制翻译结果
        textView = findViewById(R.id.transText);
        Button copy = findViewById(R.id.copy);
        copy.setOnClickListener(view -> {
            //获取剪贴板管理器：
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 创建普通字符型ClipData
            ClipData mClipData = ClipData.newPlainText("Label", textView.getText());
            // 将ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData);
        });

        ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                result -> {
                    if (result) {
                        openAlbum();
                    } else {
                        Toast.makeText(MainActivity.this,"您拒绝了这个许可",Toast.LENGTH_SHORT).show();
                    }
                });

//        打开相册
        CardView album = findViewById(R.id.album);
        album.setOnClickListener(view -> {
//        请求储存器读写权限
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                openAlbum();
            }

        });

//        创建悬浮窗
        Switch floatViewSwitch = findViewById(R.id.floatViewSwicth);
        FloatBallView floatBallView = new FloatBallView();
        floatViewSwitch.setOnCheckedChangeListener((compoundButton, open) -> {
            if (open) {
                if (!Settings.canDrawOverlays(MainActivity.this)) {
                    //若未授权则请求权限
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    floatBallView.createFloatView(MainActivity.this);
                }
                floatBallView.createFloatView(MainActivity.this);
                mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                Intent intent = mMediaProjectionManager.createScreenCaptureIntent();
                screenshotLauncher.launch(intent);
            } else {
                floatBallView.removeFloatView();
                Intent stopIntent = new Intent(MainActivity.this,ScreenshotService.class);
                stopService(stopIntent);
                unbindService(connection);
            }
        });
        floatBallView.onFloatViewClick(v -> {
            screenshotBinder.Screenshot();
        });

//        注册广播实例（在初始化的时候）
        IntentFilter filter1 = new IntentFilter();
        filter1.addAction(ACTION_SCREENSHOT_SERVICE);
        serviceBroadcastReceiver = new ServiceBroadcastReceiver();
        registerReceiver(serviceBroadcastReceiver, filter1);

        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(ACTION_OCR);
        ocrBroadcastReceiver = new OcrBroadcastReceiver();
        registerReceiver(ocrBroadcastReceiver, filter2);
    }

    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            //    打开相机
            case R.id.camera:
                File photo = new File(getExternalCacheDir(),"photo.jpg");
                Uri photoUri = FileProvider.getUriForFile(this, "com.OcrTranslator.fileprovider", photo);
                requestPhotoLauncher.launch(photoUri);
                break;
        }
        return true;
    }

//    拍照并启动OCR活动
    ActivityResultLauncher<Uri> requestPhotoLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(),
            result-> {
                if (result) {
                    imagePath = getExternalCacheDir().getPath() + "/photo.jpg";
                    ocr();
                }
            });

    //    打开相册
    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        requestAlbumLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> requestAlbumLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    assert result.getData() != null;
                    handleImageOnKitKat(result.getData());
                }
            });

    //    解析URI并启动OCR活动
    private void handleImageOnKitKat(Intent data) {
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(this,uri)){
            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())){
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content:" +
                        "//download/public/public_downloads"), Long.parseLong(docId));
                imagePath = getImagePath(contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())){
            imagePath = uri.getPath();
        }
        ocr();
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;
        Cursor cursor = getContentResolver().query(uri,null,selection,null,null);
        if (cursor != null){
            if (cursor.moveToFirst()) {
                path = cursor.getString((cursor.getColumnIndex(MediaStore.Images.Media.DATA)));
            }
            cursor.close();
        }
        return path;
    }

//    OCR活动
    private void ocr() {
        Intent intent =new Intent(MainActivity.this, OcrActivity.class);
        intent.putExtra("from", from.getSelectedItem().toString());
        intent.putExtra("to", to.getSelectedItem().toString());
        intent.putExtra("path", imagePath);
        intent.putExtra("requestScreenshot", REQUEST_SCREENSHOT);
        REQUEST_SCREENSHOT = false;
        requestOcrLauncher.launch(intent);
    }

    //        获取返回的ocr结果并写进文本框
    ActivityResultLauncher<Intent> requestOcrLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result ->  {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    assert result.getData() != null;
                    String ocrText = result.getData().getStringExtra("ocrText");
                    String resultText = result.getData().getStringExtra("resultText");
                    if (ocrText != null){
                        editText.setText(ocrText);
                    }
                    if (resultText != null){
                        textView.setText(resultText);
                    }
                }
            });

//    翻译
    private void translate(String transText, String from, String to) {
        TransUrl transUrl = new TransUrl();
        String url = transUrl.url(transText, from, to);
        final String[] result = {""};
        HttpUtil.sendOkHttpRequest(url, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                assert response.body() != null;
                List<TransResult> transResults = JsonParse.handleTransResponse(response.body().string());
                runOnUiThread(() -> {
                    try {
                        for (int i = 0; i < Objects.requireNonNull(transResults).size(); i++){
                            result[0] = result[0] + transResults.get(i).trans;
                        }
                        textView.setText(result[0]);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    ActivityResultLauncher<Intent> screenshotLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        result ->  {
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
                service.putExtra("dpi",dpi);
                service.putExtra("width",width);
                service.putExtra("height",height);
                bindService(service, connection, BIND_AUTO_CREATE);
                startForegroundService(service);
            }
    });

//    定义广播接收器，用于执行Service服务的请求（内部类）
    private class ServiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            imagePath = intent.getStringExtra("imagePath");
            REQUEST_SCREENSHOT = true;
            ocr();
        }
    }

//    定义广播接收器，用于执行来自ocr活动的请求（内部类）
    private class OcrBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }

    @Override
    public void finish() {
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }
}