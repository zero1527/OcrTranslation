package com.OcrTranslator;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.OcrTranslator.gson.OcrResult;
import com.OcrTranslator.service.OcrService;
import com.OcrTranslator.utils.UIUtil;
import com.OcrTranslator.utils.Utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class CameraActivity extends AppCompatActivity {

    private ImageView photo;

    private Uri imageUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        UIUtil.fullINScreen( getWindow());
        setContentView(R.layout.activity_camera);
        photo = findViewById(R.id.photo);

        File outputImage = new File(getExternalCacheDir(),"output_image.jpg");
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        imageUri = FileProvider.getUriForFile(this,"com.OcrTranslator.fileprovider", outputImage);
        takePhoto();
    }

    ActivityResultLauncher<Intent> requestDataLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    displayImage();
                }
            }
    });

    private void takePhoto() {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        requestDataLauncher.launch(intent);
    }

    private void displayImage() {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
            photo.setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                String imagePath = "/storage/emulated/0/Android/data/com.OcrTranslator/cache/output_image.jpg";
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

    private void ocr(List<OcrResult> result) {
        String ocrText = "";
        try {
            for (OcrResult ocrResult : result) {
                ocrText = ocrText + ocrResult.words + "\n";
            }
            Intent intent = new Intent(CameraActivity.this,MainActivity.class);
            intent.putExtra("ocrText", ocrText);
            setResult(RESULT_OK,intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}