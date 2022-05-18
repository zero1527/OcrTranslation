package com.OcrTranslator.data;


public class Language {

    private String from;

    private String to;

    public String parseFrom (String language1) {

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
            case "波兰语":
                from = "pl";
                break;
            case "意大利语":
                from = "it";
                break;
            case "荷兰语":
                from = "nl";
                break;
            default:
        }

        return from;
    }

    public String parseTo (String language2) {

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
            case "波兰语":
                to = "pl";
                break;
            case "意大利语":
                to = "it";
                break;
            case "荷兰语":
                to = "nl";
                break;
            default:
        }

        return to;
    }
}
