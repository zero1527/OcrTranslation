package com.OcrTranslator.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.preference.PreferenceManager;
import android.util.Log;

import com.OcrTranslator.MyApplication;
import com.OcrTranslator.utils.Base64Util;
import com.OcrTranslator.utils.FileUtil;
import com.OcrTranslator.utils.HttpUtil;

import java.net.URLEncoder;
import java.util.Date;

public class OcrService {

    private final String ak = "VSLduXRN2kpI65dBXA1OTKb0";

    private final String sk = "xBPkd2spSFYFkRCfXRC6dwTGXmIDKXYY";

    private Calendar time;

    public String ocrResult(String path) {

        String url = "https://aip.baidubce.com/rest/2.0/ocr/v1/general";

        SharedPreferences prefer = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());

        String accessTokenPrefs = prefer.getString("accessToken",null);

        long day = 1000 * 60 * 60 * 24;

        try {
            // 本地文件路径
            byte[] imgData = FileUtil.readFileByBytes(path);
            String imgStr = Base64Util.encode(imgData);
            String imgParam = URLEncoder.encode(imgStr, "UTF-8");

            String param = "image=" + imgParam;

            String result;
            if (time == null) {
                time = Calendar.getInstance();
            }
            if (accessTokenPrefs != null) {
                Calendar calendar = Calendar.getInstance();
                Date gTime = time.getTime();
                Date now = calendar.getTime();
                long diff = now.getTime() - gTime.getTime();
                long tf = diff / day;
                if (tf < 31) {
                    result = HttpUtil.post(url, accessTokenPrefs, param);
                    Log.d("OcrService","从缓存");
                } else {
                    String accessToken = AuthService.getAuth(ak,sk);
                    time = cache(accessToken);
                    result = HttpUtil.post(url, accessToken, param);
                    Log.d("OcrService","过期");
                }
            } else {
                String accessToken = AuthService.getAuth(ak,sk);
                time = cache(accessToken);
                result = HttpUtil.post(url, accessToken, param);
                Log.d("OcrService","从网络获取");
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Calendar cache(String accessToken) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit();
        editor.putString("accessToken", accessToken);
        editor.apply();
        return Calendar.getInstance();
    }

    public void clearCache() {
        SharedPreferences.Editor editor = MyApplication.getContext().getSharedPreferences("com.OcrTranslator_preferences", Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        Log.d("clearPrefer","清除缓存");
    }
}
