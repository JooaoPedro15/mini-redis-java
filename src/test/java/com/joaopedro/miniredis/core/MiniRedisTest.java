package com.joaopedro.miniredis.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiniRedisTest {

    @Test
    void setAndGetStoreValue() {
        MiniRedis redis = new MiniRedis();

        assertEquals("OK", redis.set("name", "John"));
        assertEquals("John", redis.get("name"));
    }

    @Test
    void delRemovesExistingKey() {
        MiniRedis redis = new MiniRedis();
        redis.set("name", "John");

        assertEquals(1, redis.del("name"));
        assertNull(redis.get("name"));
        assertEquals(0, redis.del("name"));
    }

    @Test
    void existsReturnsOneOnlyForExistingKey() {
        MiniRedis redis = new MiniRedis();

        assertEquals(0, redis.exists("name"));

        redis.set("name", "John");

        assertEquals(1, redis.exists("name"));

        redis.del("name");

        assertEquals(0, redis.exists("name"));
    }

    @Test
    void expireDefinesRelativeExpirationForExistingKey() {
        MiniRedis redis = new MiniRedis();
        redis.set("session", "active");

        assertEquals(1, redis.expire("session", 2));

        long ttl = redis.ttl("session");

        assertTrue(ttl > 0 && ttl <= 2);
        assertEquals(0, redis.expire("missing", 2));
    }

    @Test
    void expireAtDefinesAbsoluteExpirationForExistingKey() {
        MiniRedis redis = new MiniRedis();
        redis.set("session", "active");

        assertEquals(1, redis.expireAt("session", System.currentTimeMillis() + 2000));

        long ttl = redis.ttl("session");

        assertTrue(ttl > 0 && ttl <= 2);
        assertEquals(0, redis.expireAt("missing", System.currentTimeMillis() + 2000));
    }

    @Test
    void ttlReturnsMinusOneForKeyWithoutExpiration() {
        MiniRedis redis = new MiniRedis();

        redis.set("name", "John");

        assertEquals(-1, redis.ttl("name"));
    }

    @Test
    void ttlReturnsMinusTwoForMissingKey() {
        MiniRedis redis = new MiniRedis();

        assertEquals(-2, redis.ttl("missing"));
    }

    @Test
    void expiredKeyIsRemovedWhenRead() {
        MiniRedis redis = new MiniRedis();
        redis.set("temporary", "value");

        redis.expireAt("temporary", System.currentTimeMillis() - 1);

        assertNull(redis.get("temporary"));
        assertEquals(0, redis.exists("temporary"));
        assertEquals(-2, redis.ttl("temporary"));
    }

    @Test
    void flushAllRemovesAllKeys() {
        MiniRedis redis = new MiniRedis();

        redis.set("first", "1");
        redis.set("second", "2");

        redis.flushAll();

        assertNull(redis.get("first"));
        assertNull(redis.get("second"));
        assertEquals(0, redis.keys().length);
    }

    @Test
    void keysReturnsOnlyActiveKeys() {
        MiniRedis redis = new MiniRedis();

        redis.set("active", "1");
        redis.set("expired", "2");
        redis.expireAt("expired", System.currentTimeMillis() - 1);

        String[] keys = redis.keys();

        assertEquals(1, keys.length);
        assertEquals("active", keys[0]);
    }
}
