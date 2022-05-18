package com.OcrTranslator;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.OcrTranslator.data.Config;
import com.OcrTranslator.data.Const;
import com.OcrTranslator.data.Language;
import com.OcrTranslator.gson.OcrResult;
import com.OcrTranslator.http.HttpStringCallback;
import com.OcrTranslator.pic.PicTranslate;
import com.OcrTranslator.utils.UIUtil;
import com.OcrTranslator.utils.JsonParse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Base64;

public class OcrActivity extends AppCompatActivity {

    private static final String TAG = "OcrActivity";

    private String path;

    private LinearLayout switchoverLayout;

    private Spinner from;

    private Spinner to;

    private ImageView switchover;

    private String language1;

    private String language2;

    private ImageView transImage;

    private ProgressBar progressBar;

    private Button ocrTrans;

    private Button copyResult;

    private  OcrResult ocrResult;

    private Boolean REQUEST_SCREENSHOT = false;

    private Boolean RETURN_MAIN = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        UIUtil.fullINScreen(getWindow());
        setContentView(R.layout.activity_ocr);
        switchoverLayout = findViewById(R.id.switchover_layout);
        switchoverLayout.setVisibility(View.GONE);
        transImage = findViewById(R.id.transImage);
        progressBar = findViewById(R.id.transImageProgressBar);

        Intent intent = getIntent();
        if (intent != null) {
            path = intent.getStringExtra("path");
            Language language = new Language();
            language1 = language.parseFrom(intent.getStringExtra("from"));
            language2 = language.parseTo(intent.getStringExtra("to"));
            REQUEST_SCREENSHOT = intent.getBooleanExtra("requestScreenshot", false);
            displayImage(path, language1, language2);
            Log.d(TAG,path);
        }

        //        翻译语言切换
        from = findViewById(R.id.imageLanguage1);
        to = findViewById(R.id.imageLanguage2);
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(
                this,R.array.languageList1,R.layout.spinnerlayout_image);
        adapter1.setDropDownViewResource(R.layout.spinnerlayout_image_list);
        from.setAdapter(adapter1);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(
                this,R.array.languageList2,R.layout.spinnerlayout_image);
        adapter2.setDropDownViewResource(R.layout.spinnerlayout_image_list);
        to.setAdapter(adapter2);
        switchover = findViewById(R.id.imageSwitchover);
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
        from.setVisibility(View.GONE);
        to.setVisibility(View.GONE);
        switchover.setVisibility(View.GONE);

        ImageView exit = findViewById(R.id.exit);
        exit.setOnClickListener(view -> finish());

        ocrTrans = findViewById(R.id.ocrTrans);
        ocrTrans.setVisibility(View.GONE);
        ocrTrans.setOnClickListener(view -> {
            progressBar.setVisibility(View.VISIBLE);
            Language language = new Language();
            language1 = language.parseFrom(from.getSelectedItem().toString());
            language2 = language.parseTo(to.getSelectedItem().toString());
            displayImage(path, language1, language2);
        });

        copyResult = findViewById(R.id.copyResult);
        copyResult.setVisibility(View.GONE);
        copyResult.setOnClickListener(view -> {
            try {
                String ocrText = ocrResult.sumSrc;
                String resultText = ocrResult.sumDst;
                Intent intent1 = new Intent(OcrActivity.this, MainActivity.class);
                intent1.putExtra("ocrText", ocrText);
                intent1.putExtra("resultText", resultText);
                setResult(RESULT_OK, intent1);
                RETURN_MAIN = true;
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void displayImage(String imagePath, String language1, String language2) {
        if (imagePath != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            transImage.setImageBitmap(bitmap);
        } else {
            Toast.makeText(this,"获取图片失败", Toast.LENGTH_SHORT).show();
        }
        Config config = new Config(Const.APPID, Const.SECRET_KEY);
        config.lang(language1, language2);
        config.pic(imagePath);
        config.erase(Config.ERASE_NONE);
        config.paste(Config.PASTE_FULL);

        PicTranslate picTranslate = new PicTranslate();
        picTranslate.setConfig(config);

        picTranslate.trans(new HttpStringCallback() {
            @Override
            protected void onSuccess(String response) {
                super.onSuccess(response);
                Log.d(TAG,response);
                ocrResult = JsonParse.handleOcrResultResponse(response);

                runOnUiThread(() -> {
                    Base64.Decoder decoder = Base64.getDecoder();
                    try {
                        //Base64解码
                        byte[] b = decoder.decode(ocrResult.pasteImg);
                        // 处理数据
                        for (int i = 0; i < b.length; ++i) {
                            //调整异常数据
                            if (b[i] < 0) {
                                b[i] += 256;
                            }
                        }
                        File ocrTransImage = new File(getExternalCacheDir(),"ocrTrans.jpg");
                        OutputStream out = new FileOutputStream(ocrTransImage);
                        out.write(b);
                        out.flush();
                        out.close();
                        transImage.setImageURI(Uri.fromFile(ocrTransImage));
                        switchoverLayout.setVisibility(View.VISIBLE);
                        from.setVisibility(View.VISIBLE);
                        to.setVisibility(View.VISIBLE);
                        switchover.setVisibility(View.VISIBLE);
                        ocrTrans.setVisibility(View.VISIBLE);
                        copyResult.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    } catch (Exception ignored) {
                    }
                });
            }

            @Override
            protected void onFailure(Throwable e) {
                super.onFailure(e);
            }
        });
    }

    @Override
    public void finish() {
        if (!RETURN_MAIN && REQUEST_SCREENSHOT) {
            Intent intent = new Intent();
            intent.setAction(MainActivity.ACTION_OCR);
            sendBroadcast(intent);
        }
        super.finish();
    }
}