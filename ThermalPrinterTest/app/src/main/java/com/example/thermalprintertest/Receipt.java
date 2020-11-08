package com.example.thermalprintertest;

import android.util.DisplayMetrics;

public class Receipt {
    private String header;
    private String body;
    private String footer;

    public Receipt(){

    }

    public String getHeader(){
        return this.header;
    }

    public void setHeader(String header){
        this.header = header;
    }

    public String getFooter(){
        return this.footer;
    }

    public void setFooter(String footer){
        this.footer = footer;
    }
}
