package com.OcrTranslator;

import android.util.Log;

import com.OcrTranslator.data.Const;
import com.OcrTranslator.data.Language;
import com.OcrTranslator.utils.MD5;

import java.net.URLEncoder;

public class TransUrl {

    public String url(String q, String language1, String language2) {

        String url = Const.TRANS_URL;

        String ak = Const.APPID;

        String sk = Const.SECRET_KEY;

        Language language = new Language();
        String from = language.parseFrom(language1);
        String to = language.parseTo(language2);

        try {
            String q2 = URLEncoder.encode(q, "UTF-8").replace("+", "%20");
            String salt = String.valueOf(System.currentTimeMillis());
            String a = ak + q + salt + sk;
            String sign = MD5.md5(a);
            String address = url + "?q=" + q2 + "&from=" + from + "&to=" + to +
                    "&appid=" + ak + "&salt=" + salt + "&sign=" +sign;
            Log.d("TransResult",address);
            return address;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
