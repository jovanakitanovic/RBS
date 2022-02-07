package com.zuehlke.securesoftwaredevelopment.config;

public class CSRFTokenMissMatch extends Exception{
    
    private String errorText;
    public CSRFTokenMissMatch(String text){
        errorText=text;
    }
    
    public String getErrorText(){
        return errorText;
    }
}
