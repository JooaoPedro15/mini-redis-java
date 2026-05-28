package com.joaopedro.miniredis.core;

public class Entry
{
    private String value;
    private Long expiresAt;

    // Creates a Mini Redis value entry.
    // Stores the received String and starts without an expiration timestamp.
    public Entry(String value)
    {
        this.value = value;
        this.expiresAt = null;
    }

    // Returns the value stored in the entry.
    // This method returns only the String, without touching expiration metadata.
    public String getValue()
    {
        return value;
    }

    // Updates the value stored in the entry.
    // Replaces only the String and preserves the current expiration timestamp.
    public void setValue(String value)
    {
        this.value = value;
    }

    // Returns the expiration timestamp of the entry.
    // Returns null when the key has no TTL configured.
    public Long getExpiresAt()
    {
        return expiresAt;
    }

    // Sets the expiration timestamp of the entry.
    // Receives a value in milliseconds used to check if the key has expired.
    public void setExpiresAt(Long expiresAt)
    {
        this.expiresAt = expiresAt;
    }

    // Checks whether the entry has already expired.
    // Compares the current timestamp with expiresAt and returns false when no expiration is set.
    public boolean isExpired()
    {
        boolean result = false;

        if (expiresAt != null && System.currentTimeMillis() >= expiresAt)
        {
            result = true;
        }

        return result;
    }
}
