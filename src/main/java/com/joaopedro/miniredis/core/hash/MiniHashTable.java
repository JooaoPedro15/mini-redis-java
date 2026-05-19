package com.joaopedro.miniredis.core.hash;

import com.joaopedro.miniredis.core.Entry;

public class MiniHashTable
{
    private static final int DEFAULT_CAPACITY = 16;

    private HashNode[] buckets;
    private int size;

    public MiniHashTable()
    {
        this.buckets = new HashNode[DEFAULT_CAPACITY];
        this.size = 0;
    }

    public void put(String key, Entry value)
    {
        validateKey(key);

        int index = getIndex(key);
        HashNode current = buckets[index];

        while (current != null && !keysAreEqual(current.getKey(), key))
        {
            current = current.getNext();
        }

        if (current != null)
        {
            current.setValue(value);
        }
        else
        {
            HashNode newNode = new HashNode(key, value);

            newNode.setNext(buckets[index]);
            buckets[index] = newNode;

            size++;
        }
    }

    public Entry get(String key)
    {
        validateKey(key);

        Entry result = null;

        int index = getIndex(key);
        HashNode current = buckets[index];

        while (current != null && result == null)
        {
            if (keysAreEqual(current.getKey(), key))
            {
                result = current.getValue();
            }
            else
            {
                current = current.getNext();
            }
        }

        return result;
    }

    public Entry remove(String key)
    {
        validateKey(key);

        Entry result = null;

        int index = getIndex(key);
        HashNode current = buckets[index];
        HashNode previous = null;

        boolean found = false;

        while (current != null && !found)
        {
            if (keysAreEqual(current.getKey(), key))
            {
                found = true;
                result = current.getValue();

                if (previous == null)
                {
                    buckets[index] = current.getNext();
                }
                else
                {
                    previous.setNext(current.getNext());
                }

                size--;
            }
            else
            {
                previous = current;
                current = current.getNext();
            }
        }

        return result;
    }

    public boolean containsKey(String key)
    {
        boolean result = false;

        if (get(key) != null)
        {
            result = true;
        }

        return result;
    }

    public int size()
    {
        return size;
    }

    public int capacity()
    {
        return buckets.length;
    }

    private int getIndex(String key)
    {
        int index = hash(key) % buckets.length;

        return index;
    }

    private int hash(String key)
    {
        int result = 0;

        for (int i = 0; i < key.length(); i++)
        {
            result = 31 * result + key.charAt(i);
        }

        result = result & 0x7fffffff;

        return result;
    }

    private boolean keysAreEqual(String a, String b)
    {
        boolean result = false;

        if (a != null && b != null && a.length() == b.length())
        {
            result = true;

            for (int i = 0; i < a.length(); i++)
            {
                if (a.charAt(i) != b.charAt(i))
                {
                    result = false;
                }
            }
        }

        return result;
    }

    private void validateKey(String key)
    {
        if (key == null || key.length() == 0)
        {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
    }
}