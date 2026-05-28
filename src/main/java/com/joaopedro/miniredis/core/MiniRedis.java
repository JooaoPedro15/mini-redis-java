package com.joaopedro.miniredis.core;

import com.joaopedro.miniredis.core.hash.MiniHashTable;
import com.joaopedro.miniredis.core.hash.HashEntry;

public class MiniRedis
{
    private MiniHashTable data;

    // Creates the in-memory database.
    // Initializes the manual hash table used to store keys and values.
    public MiniRedis()
    {
        this.data = new MiniHashTable();
    }

    // Stores a key with a value in the database.
    // Creates a new Entry without expiration and puts that Entry into the hash table.
    public String set(String key, String value)
    {
        data.put(key, new Entry(value));

        return "OK";
    }

    // Looks up the value of a key in the database.
    // First removes the key if it has already expired, then fetches the Entry from
    // the hash table.
    public String get(String key)
    {
        String result = null;

        removeIfExpired(key);

        Entry entry = data.get(key);

        if (entry != null)
        {
            result = entry.getValue();
        }

        return result;
    }

    // Removes a key from the database.
    // First checks whether the key has expired, then tries to remove it from the
    // hash table. Returns 1 if removed and 0 if the key did not exist.
    public int del(String key)
    {
        int result = 0;

        removeIfExpired(key);

        Entry removed = data.remove(key);

        if (removed != null)
        {
            result = 1;
        }

        return result;
    }

    // Checks whether a key exists in the database.
    // First removes the key if it has expired, then queries the hash table.
    public int exists(String key)
    {
        int result = 0;

        removeIfExpired(key);

        if (data.containsKey(key))
        {
            result = 1;
        }

        return result;
    }

    // Sets a relative expiration for a key.
    // Computes the future moment in milliseconds and delegates to expireAt to
    // store that absolute timestamp.
    public int expire(String key, long seconds)
    {
        int result = 0;

        long expiresAt = System.currentTimeMillis() + seconds * 1000;

        result = expireAt(key, expiresAt);

        return result;
    }

    // Returns the remaining time-to-live of a key.
    // Returns -2 if the key does not exist, -1 if it exists without expiration,
    // or the remaining seconds otherwise.
    public long ttl(String key)
    {
        long result = -2;

        removeIfExpired(key);

        Entry entry = data.get(key);

        if (entry != null)
        {
            if (entry.getExpiresAt() == null)
            {
                result = -1;
            }
            else
            {
                long millisLeft = entry.getExpiresAt() - System.currentTimeMillis();

                if (millisLeft > 0)
                {
                    result = millisLeft / 1000;

                    if (result == 0)
                    {
                        result = 1;
                    }
                }
                else
                {
                    data.remove(key);
                    result = -2;
                }
            }
        }

        return result;
    }

    // Removes a key if it has already passed its expiration timestamp.
    // Fetches the Entry from the hash table and removes it from the database if
    // it is expired.
    private void removeIfExpired(String key)
    {
        Entry entry = data.get(key);

        if (entry != null && entry.isExpired())
        {
            data.remove(key);
        }
    }

    // Sets an absolute expiration timestamp for a key.
    // Receives the timestamp in milliseconds and stores it inside the Entry.
    public int expireAt(String key, long expiresAt)
    {
        int result = 0;

        removeIfExpired(key);

        Entry entry = data.get(key);

        if (entry != null)
        {
            entry.setExpiresAt(expiresAt);

            result = 1;
        }

        return result;
    }

    // Returns every valid entry currently stored in the database.
    // First removes expired keys, then returns only the entries that are still active.
    public HashEntry[] entries()
    {
        HashEntry[] allEntries = data.entries();
        HashEntry[] temporary = new HashEntry[allEntries.length];

        int count = 0;

        for (int i = 0; i < allEntries.length; i++)
        {
            HashEntry current = allEntries[i];

            if (current != null && current.getValue() != null)
            {
                if (current.getValue().isExpired())
                {
                    data.remove(current.getKey());
                }
                else
                {
                    temporary[count] = current;
                    count++;
                }
            }
        }

        HashEntry[] result = new HashEntry[count];

        for (int i = 0; i < count; i++)
        {
            result[i] = temporary[i];
        }

        return result;
    }

    // Removes every key from the database.
    // Calls clear on the hash table to wipe all in-memory data.
    public void flushAll()
    {
        data.clear();
    }

    // Returns every active key in the database.
    // Walks through the hash table keys, removes the expired ones and returns only
    // the keys that are still valid.
    public String[] keys()
    {
        String[] allKeys = data.keys();
        String[] temporary = new String[allKeys.length];

        int count = 0;

        for (int i = 0; i < allKeys.length; i++)
        {
            String key = allKeys[i];

            if (key != null)
            {
                removeIfExpired(key);

                if (data.containsKey(key))
                {
                    temporary[count] = key;
                    count++;
                }
            }
        }

        String[] result = new String[count];

        for (int i = 0; i < count; i++)
        {
            result[i] = temporary[i];
        }

        return result;
    }
}
