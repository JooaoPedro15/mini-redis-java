package com.joaopedro.miniredis.core;

public class Entry {
    
    private String value;
    private Long expiresAt;


    public Entry ( String value){

        this.value = value;
        this.expiresAt = null; 
    }

    public String getValue (){
        return value;
    }

    public void setValue ( String value){
        this.value = value;
    }

    public Long getExpiresAt (){
        return expiresAt;
    }

    public void setExpiresAt ( Long expiresAt ){
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(){
        
        boolean resultado = false;

        if ( expiresAt != null && System.currentTimeMillis() >= expiresAt )
        {
            resultado = true;
        }

        return resultado;
    }
    


}
