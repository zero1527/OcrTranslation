package com.OcrTranslator.gson;

import com.google.gson.annotations.SerializedName;

public class TransResult {

    @SerializedName("src")
    public String original;

    @SerializedName("dst")
    public String trans;

}
