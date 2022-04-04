package com.OcrTranslator;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;;

import com.OcrTranslator.gson.OcrResult;
import com.OcrTranslator.gson.TransResult;
import com.OcrTranslator.service.OcrService;
import com.OcrTranslator.service.TransService;
import com.OcrTranslator.utils.HttpUtil;
import com.OcrTranslator.utils.UIUtil;
import com.OcrTranslator.utils.Utility;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class TransImageActivity extends AppCompatActivity {

    private String TAG = "TransImageActivity";

    private Uri uri;

    private String path;

    private String from;

    private String to;

    private ImageView transImage;

    private List<String> ocrLocationTop = new ArrayList<>();

    private List<String> ocrLocationLeft = new ArrayList<>();

    private ConstraintLayout transLayout;

    private ProgressBar progressBar;

    //声明一个操作常量字符串
//    public static final String ACTION_SERVICE_NEED = "action.ServiceNeed";
    //声明一个内部广播实例
//    public ScreenshotActivity.ServiceNeedBroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        UIUtil.fullINScreen(getWindow());
        setContentView(R.layout.activity_trans_image);
        transImage = findViewById(R.id.transImage);
        transLayout = findViewById(R.id.transLayout);

        Intent intent = getIntent();
        if (intent != null) {
            uri = intent.getParcelableExtra("uri");
            path = intent.getStringExtra("path");
            from = intent.getStringExtra("from");
            to = intent.getStringExtra("to");
            if (path == null) {
                path = uri.getPath();
            }
            displayImage(path, from, to);
            Log.d(TAG,path);
//            screenshot.setImageURI(uri);
//            Glide.with(ScreenshotActivity.this).clear(imageView);
//            Glide.with(this).load(uri).into(imageView);
//            Log.d(TAG, "打开图片");
        }

        Button exit = findViewById(R.id.exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

//    @Override
//    public void onBackPressed() {
////        super.onBackPressed();
//        finish();
//    }

    private void displayImage(String imagePath, String language1, String language2) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            transImage.setImageBitmap(bitmap);
            progressBar = findViewById(R.id.transImageProgressBar);
        } else {
            Toast.makeText(this,"获取图片失败", Toast.LENGTH_SHORT).show();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                OcrService ocrService = new OcrService();
                String ocrResult = ocrService.ocrResult(imagePath);
                List<OcrResult> ocrResults = Utility.handleOcrResultResponse(ocrResult);
                ocr(ocrResults, language1, language2);
            }
        }).start();
    }

    private void ocr(List<OcrResult> result, String language1, String language2){
        List<String> text = new ArrayList<>();
        try {
            for (OcrResult ocrResult : result) {
                text.add(ocrResult.words);
                ocrLocationTop.add(ocrResult.location.top);
                ocrLocationLeft.add(ocrResult.location.left);
            }
            trans(text, language1, language2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void trans(List<String> transText, String language1, String language2) {
        TransService transService = new TransService();
        String address = transService.TransResult(transText,language1,language2);
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
                        assert transResults != null;
                        displayTrans(transResults);
                    }
                });
            }
        });
    }

    public void displayTrans(List<TransResult> transResults) {
        transLayout.removeAllViews();
        for (int i = 0; i < transResults.size(); i++){
            View view = LayoutInflater.from(this).inflate(R.layout.trans, transLayout,false);
            TextView text = view.findViewById(R.id.trans);
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) text.getLayoutParams();
            lp.setMargins(Integer.parseInt(ocrLocationLeft.get(i)), Integer.parseInt(ocrLocationTop.get(i)), 0, 0);
            text.setText(transResults.get(i).trans);
            text.setTextSize(16);
            transLayout.addView(view);
            progressBar.setVisibility(View.GONE);
        }
    }

//    private class ServiceNeedBroadcastReceiver extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            Log.d(TAG, "接收截图");
            //这里是要在Activity活动里执行的代码
//            uri = intent.getParcelableExtra("uri");
//            imageView.clearFocus();
//            Log.d(TAG, "打开图片");
//            Glide.with(ScreenshotActivity.this).load(uri).into(imageView);
//            Log.d(TAG, "展示图片");
//        }
//    }
}
