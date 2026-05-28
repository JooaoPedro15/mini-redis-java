package com.joaopedro.miniredis.core.hash;

import com.joaopedro.miniredis.core.Entry;

public class MiniHashTable
{
    private static final int DEFAULT_CAPACITY = 16;
    private static final double MAX_LOAD_FACTOR = 0.75;

    private HashNode[] buckets;
    private int size;

    // Creates an empty hash table.
    // Allocates the bucket array with the default capacity and sets size to zero.
    public MiniHashTable()
    {
        this.buckets = new HashNode[DEFAULT_CAPACITY];
        this.size = 0;
    }

    // Inserts or updates a key in the hash table.
    // First validates the key and looks for an existing node. If found, updates
    // the value. Otherwise, resizes the table if needed and creates a new node
    // at the head of the target bucket.
    public synchronized void put(String key, Entry value)
    {
        validateKey(key);

        HashNode current = findNode(key);

        if (current != null)
        {
            current.setValue(value);
        }
        else
        {
            if (shouldResize())
            {
                resize();
            }

            int index = getIndex(key);

            HashNode newNode = new HashNode(key, value);

            newNode.setNext(buckets[index]);
            buckets[index] = newNode;

            size++;
        }
    }

    // Looks up a key in the hash table.
    // Uses findNode to locate the correct node inside the bucket computed by the
    // hash function. Returns the associated Entry, or null when missing.
    public synchronized Entry get(String key)
    {
        validateKey(key);

        Entry result = null;

        HashNode node = findNode(key);

        if (node != null)
        {
            result = node.getValue();
        }

        return result;
    }

    // Removes a key from the hash table.
    // Computes the index and walks the bucket's linked list using two pointers,
    // previous and current. When the key is found, unlinks the node from the list
    // and returns the removed Entry.
    public synchronized Entry remove(String key)
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

    // Checks whether a key exists in the hash table.
    // Delegates to get and returns true when a non-null Entry is found.
    public synchronized boolean containsKey(String key)
    {
        boolean result = false;

        if (get(key) != null)
        {
            result = true;
        }

        return result;
    }

    // Returns the number of keys currently stored.
    // The value is updated whenever a new key is inserted or an existing one is removed.
    public synchronized int size()
    {
        return size;
    }

    // Returns the current capacity of the table.
    // The capacity is the size of the bucket array.
    public synchronized int capacity()
    {
        return buckets.length;
    }

    // Searches for a node by key inside the table.
    // First computes the key's index and then walks the linked list of that bucket.
    private HashNode findNode(String key)
    {
        int index = getIndex(key);
        HashNode current = buckets[index];

        while (current != null && !keysAreEqual(current.getKey(), key))
        {
            current = current.getNext();
        }

        return current;
    }

    // Computes the index where a key should live.
    // First hashes the key and then takes the remainder by the array capacity.
    private int getIndex(String key)
    {
        int index = hash(key) % buckets.length;

        return index;
    }

    // Produces an integer hash from a String.
    // Walks each character of the key and accumulates a value using multiplication
    // by 31. The result is masked to stay non-negative.
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

    // Compares two Strings manually.
    // First checks that both exist and have the same length, then compares them
    // character by character.
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

    // Checks whether the table needs to grow.
    // Computes the load factor considering the next insertion and compares it
    // against the configured maximum.
    private boolean shouldResize()
    {
        boolean result = false;

        double loadFactor = (double) (size + 1) / buckets.length;

        if (loadFactor > MAX_LOAD_FACTOR)
        {
            result = true;
        }

        return result;
    }

    // Doubles the capacity of the hash table.
    // Allocates a new bucket array twice as large and reinserts every node,
    // recomputing the index for the new capacity.
    private void resize()
    {
        HashNode[] oldBuckets = buckets;

        buckets = new HashNode[oldBuckets.length * 2];
        size = 0;

        for (int i = 0; i < oldBuckets.length; i++)
        {
            HashNode current = oldBuckets[i];

            while (current != null)
            {
                HashNode next = current.getNext();

                current.setNext(null);
                insertExistingNode(current);

                current = next;
            }
        }
    }

    // Inserts an existing node into the new table during a resize.
    // Recomputes the key's index against the new capacity and pushes the node to
    // the head of the bucket.
    private void insertExistingNode(HashNode node)
    {
        int index = getIndex(node.getKey());

        node.setNext(buckets[index]);
        buckets[index] = node;

        size++;
    }

    // Validates whether a key can be used in the table.
    // Rejects null or empty keys because the hash function depends on the key's
    // characters.
    private void validateKey(String key)
    {
        if (key == null || key.length() == 0)
        {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
    }

    // Returns every entry currently stored in the hash table.
    // Walks all buckets and copies each pair found into an array of HashEntry.
    public synchronized HashEntry[] entries()
    {
        HashEntry[] result = new HashEntry[size];

        int position = 0;

        for (int i = 0; i < buckets.length; i++)
        {
            HashNode current = buckets[i];

            while (current != null)
            {
                result[position] = new HashEntry(current.getKey(), current.getValue());
                position++;

                current = current.getNext();
            }
        }

        return result;
    }

    // Removes every key from the hash table.
    // Allocates a fresh bucket array with the default capacity and resets size.
    public synchronized void clear()
    {
        this.buckets = new HashNode[DEFAULT_CAPACITY];
        this.size = 0;
    }

    // Returns every key currently stored in the hash table.
    // Walks all buckets and copies each key into an array of String.
    public synchronized String[] keys()
    {
        String[] result = new String[size];

        int position = 0;

        for (int i = 0; i < buckets.length; i++)
        {
            HashNode current = buckets[i];

            while (current != null)
            {
                result[position] = current.getKey();
                position++;

                current = current.getNext();
            }
        }

        return result;
    }
}
