package com.joaopedro.miniredis.core.hash;

import com.joaopedro.miniredis.core.Entry;

public class HashNode
{
    private String key;
    private Entry value;
    private HashNode next;

    // Creates a node of a bucket's linked list.
    // Stores the key, the associated Entry and starts with no next node.
    public HashNode(String key, Entry value)
    {
        this.key = key;
        this.value = value;
        this.next = null;
    }

    // Returns the key stored in this node.
    // Used to compare entries during lookups and removals.
    public String getKey()
    {
        return key;
    }

    // Returns the Entry stored in this node.
    // The Entry holds the actual value and the expiration metadata.
    public Entry getValue()
    {
        return value;
    }

    // Updates the Entry stored in this node.
    // Used when a put receives a key that already exists.
    public void setValue(Entry value)
    {
        this.value = value;
    }

    // Returns the next node in the linked list.
    // Allows walking through collisions inside the same bucket.
    public HashNode getNext()
    {
        return next;
    }

    // Sets the next node in the linked list.
    // Used to connect or disconnect nodes during insertion, removal and rehashing.
    public void setNext(HashNode next)
    {
        this.next = next;
    }
}
