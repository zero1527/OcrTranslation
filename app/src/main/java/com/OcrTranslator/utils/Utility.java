package com.OcrTranslator.utils;

import android.text.TextUtils;
import android.util.Log;

import com.OcrTranslator.gson.OcrResult;
import com.OcrTranslator.gson.TransResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class Utility {

    public static OcrResult handleOcrResultResponse(String response) {
        Log.d("handleOcrResultResponse",response);
        if(!TextUtils.isEmpty(response)){
            try {
                Gson gson = new Gson();
                JSONObject result = new JSONObject(response);
                String data = result.getString("data");
                OcrResult ocrResult = gson.fromJson(data, OcrResult.class);
                return ocrResult;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static List<TransResult> handleTransResultResponse(String response) {
        Log.d("handleTransResultResponse",response);
        if(!TextUtils.isEmpty(response)){
            try {
                Gson gson = new Gson();
                JSONObject jsonObject1 = new JSONObject(response);
                String transResult = jsonObject1.getString("trans_result");
                List<TransResult> transResults = gson.fromJson(transResult, new TypeToken<List<TransResult>>(){}.getType());
                return transResults;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
