package com.nhatnq.simpleapp.model;

public class GeocomplyUrlItem {
    //Title of web page
    private String title;
    //URL of web page
    private String url;

    public GeocomplyUrlItem(String title, String url){
        this.title = title;
        this.url = url;
    }

    public String getTitle(){
        return this.title;
    }

    public String getUrl(){
        return this.url;
    }

}
