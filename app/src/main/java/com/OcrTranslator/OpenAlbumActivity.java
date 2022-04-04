package com.OcrTranslator;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.OcrTranslator.gson.OcrResult;
import com.OcrTranslator.service.OcrService;
import com.OcrTranslator.utils.UIUtil;
import com.OcrTranslator.utils.Utility;

import java.util.List;

public class OpenAlbumActivity extends AppCompatActivity {

    private String TAG = "OpenAlbumActivity";

    private ImageView albumImage;

//    private String from;
//
//    private String to;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        UIUtil.fullINScreen(getWindow());
        setContentView(R.layout.activity_open_album);
        albumImage = findViewById(R.id.albumImage);
//        Intent intent = getIntent();
//        from = intent.getStringExtra("from");
//        to = intent.getStringExtra("to");

        ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (result) {
                        openAlbum();
                    } else {
                        Toast.makeText(OpenAlbumActivity.this,"您拒绝了这个许可",Toast.LENGTH_SHORT).show();
                    }
                }
        });

//        请求储存器读写权限
        if (ContextCompat.checkSelfPermission(OpenAlbumActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            openAlbum();
        }
    }

    ActivityResultLauncher<Intent> requestDataLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    assert result.getData() != null;
                    handleImageOnKitKat(result.getData());
                }
            }
    });

//    打开相册
    private void openAlbum() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        requestDataLauncher.launch(intent);
    }

//    解析URI
    private void handleImageOnKitKat(Intent data) {
        String imagePath = null;
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
        displayImage(imagePath);

//        Intent intent = new Intent(OpenAlbumActivity.this, TransImageActivity.class);
//        intent.putExtra("path", imagePath);
//        intent.putExtra("from", from);
//        intent.putExtra("to", to);
//        startActivity(intent);
    }

//    展示图片
    private void displayImage(String imagePath) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            albumImage.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this,"获取图片失败", Toast.LENGTH_SHORT).show();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                OcrService ocrService = new OcrService();
                String ocrResult = ocrService.ocrResult(imagePath);
                List<OcrResult> ocrResults = Utility.handleOcrResultResponse(ocrResult);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ocr(ocrResults);
                    }
                });
            }
        }).start();
    }

//    识别文字并返回
    private void ocr(List<OcrResult> result){
        String ocrText = "";
        try {
            for (OcrResult ocrResult : result) {
                ocrText = ocrText + ocrResult.words + "\n";
            }
            Intent intent = new Intent(OpenAlbumActivity.this, MainActivity.class);
            intent.putExtra("ocrText", ocrText);
            setResult(RESULT_OK,intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}