package com.OcrTranslator.service;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.OcrTranslator.MyApplication;
import com.OcrTranslator.R;
import com.OcrTranslator.utils.MD5;

import java.net.URLEncoder;
import java.util.List;

public class TransService {

    Context context = MyApplication.getContext();

    public String TransResult(List<String> text, String language1, String language2) {

        String url = context.getString(R.string.url);

        String ak = context.getString(R.string.ak);

        String sk = context.getString(R.string.sk);

        String from = "";
        switch (language1) {
            case "自动检测":
                from = "auto";
                break;
            case "中文":
                from = "zh";
                break;
            case "英语":
                from = "en";
                break;
            case "日语":
                from = "jp";
                break;
            case "韩语":
                from = "kor";
                break;
            case "俄语":
                from = "ru";
                break;
            case "法语":
                from = "fra";
                break;
            case "德语":
                from = "de";
                break;
            case "西班牙语":
                from = "spa";
                break;
            case "葡萄牙语":
                from = "pt";
                break;
            case "阿拉伯语":
                from = "ara";
                break;
            case "印尼语":
                from = "id";
                break;
            case "土耳其语":
                from = "tr";
                break;
            case "希腊语":
                from = "el";
                break;
//            case "繁体中文":
//                from = "cht";
//                break;
//            case "文言文":
//                from = "wyw";
//                break;
            default:
        }

        String to = "";
        switch (language2) {
            case "中文":
                to = "zh";
                break;
            case "英语":
                to = "en";
                break;
            case "日语":
                to = "jp";
                break;
            case "韩语":
                to = "kor";
                break;
            case "俄语":
                to = "ru";
                break;
            case "法语":
                to = "fra";
                break;
            case "德语":
                to = "de";
                break;
            case "西班牙语":
                to = "spa";
                break;
            case "葡萄牙语":
                to = "pt";
                break;
            case "阿拉伯语":
                to = "ara";
                break;
            case "印尼语":
                to = "id";
                break;
            case "土耳其语":
                to = "tr";
                break;
            case "希腊语":
                to = "el";
                break;
//            case "繁体中文":
//                to = "cht";
//                break;
//            case "文言文":
//                to = "wyw";
//                break;
            default:
        }

        try {
            StringBuilder q = new StringBuilder();
            for (int i = 0;i < text.size(); i++){
                q.append(text.get(i)).append("\n");
            }
            Log.d("TransTask", q.toString());
            String q2 = URLEncoder.encode(q.toString(), "UTF-8").replace("+", "%20");
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
