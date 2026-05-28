package com.joaopedro.miniredis.core.hash;

import com.joaopedro.miniredis.core.Entry;

public class HashEntry
{
    private String key;
    private Entry value;

    // Creates an exportable hash table entry.
    // Stores a key and the Entry associated with that key.
    public HashEntry(String key, Entry value)
    {
        this.key = key;
        this.value = value;
    }

    // Returns the key stored in this entry.
    // Used to iterate over the current data in the hash table.
    public String getKey()
    {
        return key;
    }

    // Returns the value stored in this entry.
    // Used to access the key's value and expiration metadata.
    public Entry getValue()
    {
        return value;
    }
}
