package com.getcapacitor.plugin.http;

public class Http {

    static {
        System.loadLibrary("ssign");
    }
    
    public static native String signParams(String str);
    
}
