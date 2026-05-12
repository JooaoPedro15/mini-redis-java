package com.joaopedro.miniredis.core.hash;

import com.joaopedro.miniredis.core.Entry;

public class HashNode {
    
    private String key; 

    private Entry value;

    private HashNode proximo;

    public HashNode ( String key, Entry value)
    {
        this.key = key;
        this.value = value;
        this.proximo = null;
    }

    public String getKey()
    {
        return key;
    }

    public Entry getValue()
    {
        return value;
    }

    public void setValue(Entry value)
    {
        this.value = value;
    }

    public HashNode getProximo()
    {
        return proximo;
    }

    public void setProximo(HashNode proximo)
    {
        this.proximo = proximo;
    }

}
