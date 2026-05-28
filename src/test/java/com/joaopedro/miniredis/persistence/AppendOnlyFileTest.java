package com.joaopedro.miniredis.persistence;

import com.joaopedro.miniredis.core.MiniRedis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppendOnlyFileTest
{
    @TempDir
    Path tempDir;

    @Test
    void loadRestoresSetCommands()
    {
        AppendOnlyFile aof = newAof();
        MiniRedis redis = new MiniRedis();

        aof.append("SET name John");
        aof.append("SET message hello world");

        aof.load(redis);

        assertEquals("John", redis.get("name"));
        assertEquals("hello world", redis.get("message"));
    }

    @Test
    void loadRestoresDelCommands()
    {
        AppendOnlyFile aof = newAof();
        MiniRedis redis = new MiniRedis();

        aof.append("SET name John");
        aof.append("DEL name");

        aof.load(redis);

        assertNull(redis.get("name"));
        assertEquals(0, redis.exists("name"));
    }

    @Test
    void loadRestoresExpireAtCommands()
    {
        AppendOnlyFile aof = newAof();
        MiniRedis redis = new MiniRedis();
        long expiresAt = System.currentTimeMillis() + 5000;

        aof.append("SET session active");
        aof.append("EXPIREAT session " + expiresAt);

        aof.load(redis);

        long ttl = redis.ttl("session");

        assertEquals("active", redis.get("session"));
        assertTrue(ttl > 0 && ttl <= 5);
    }

    @Test
    void loadRestoresFlushAllCommands()
    {
        AppendOnlyFile aof = newAof();
        MiniRedis redis = new MiniRedis();

        aof.append("SET first 1");
        aof.append("FLUSHALL");
        aof.append("SET second 2");

        aof.load(redis);

        assertNull(redis.get("first"));
        assertEquals("2", redis.get("second"));
    }

    @Test
    void rewriteRemovesOldHistoryAndKeepsCurrentState() throws IOException
    {
        AppendOnlyFile aof = newAof();
        MiniRedis redis = new MiniRedis();

        aof.append("SET name old");
        aof.append("SET unused value");
        redis.set("name", "current");

        aof.rewrite(redis);

        List<String> lines = readAofLines();

        assertEquals(1, lines.size());
        assertTrue(lines.contains("SET name current"));
        assertFalse(lines.contains("SET name old"));
        assertFalse(lines.contains("SET unused value"));
    }

    @Test
    void rewriteKeepsExpirationForActiveKey() throws IOException
    {
        AppendOnlyFile aof = newAof();
        MiniRedis redis = new MiniRedis();
        long expiresAt = System.currentTimeMillis() + 5000;

        redis.set("session", "active");
        redis.expireAt("session", expiresAt);

        aof.rewrite(redis);

        List<String> lines = readAofLines();

        assertTrue(lines.contains("SET session active"));
        assertTrue(lines.contains("EXPIREAT session " + expiresAt));
    }

    @Test
    void rewriteDoesNotKeepExpiredKeys() throws IOException
    {
        AppendOnlyFile aof = newAof();
        MiniRedis redis = new MiniRedis();

        redis.set("active", "1");
        redis.set("expired", "2");
        redis.expireAt("expired", System.currentTimeMillis() - 1);

        aof.rewrite(redis);

        List<String> lines = readAofLines();

        assertEquals(1, lines.size());
        assertTrue(lines.contains("SET active 1"));
        assertFalse(lines.contains("SET expired 2"));
    }

    @Test
    void testsUseTemporaryAofPath()
    {
        Path aofPath = aofPath();

        assertTrue(aofPath.startsWith(tempDir));
        assertFalse(aofPath.toString().contains("data"));
    }

    private AppendOnlyFile newAof()
    {
        return new AppendOnlyFile(aofPath().toString());
    }

    private Path aofPath()
    {
        return tempDir.resolve("appendonly.aof");
    }

    private List<String> readAofLines() throws IOException
    {
        return Files.readAllLines(aofPath());
    }
}
