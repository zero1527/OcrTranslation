package com.OcrTranslator.gson;

public class OcrResult {

    public String words;

    public Location location;

    public class Location {

        public String top;

        public String left;

        public String width;

        public String height;

    }

    public void setWords(String words) {
        this.words = words;
    }
}
