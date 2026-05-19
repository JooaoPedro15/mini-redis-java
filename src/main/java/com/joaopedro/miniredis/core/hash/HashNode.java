package com.joaopedro.miniredis.core.hash;

import com.joaopedro.miniredis.core.Entry;

public class HashNode
{
    private String key;
    private Entry value;
    private HashNode next;

    public HashNode(String key, Entry value)
    {
        this.key = key;
        this.value = value;
        this.next = null;
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

    public HashNode getNext()
    {
        return next;
    }

    public void setNext(HashNode next)
    {
        this.next = next;
    }
}