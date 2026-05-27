package com.joaopedro.miniredis.command;

import com.joaopedro.miniredis.core.MiniRedis;
import com.joaopedro.miniredis.persistence.AppendOnlyFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void pingReturnsPong() {
        CommandProcessor processor = newProcessor();

        assertEquals("PONG", processor.process("PING"));
    }

    @Test
    void setAndGetWorkThroughCommands() {
        CommandProcessor processor = newProcessor();

        assertEquals("OK", processor.process("SET name John"));
        assertEquals("John", processor.process("GET name"));
    }

    @Test
    void setKeepsValueWithSpaces() {
        CommandProcessor processor = newProcessor();

        assertEquals("OK", processor.process("SET message hello world"));
        assertEquals("hello world", processor.process("GET message"));
    }

    @Test
    void commandsAcceptExtraSpaces() {
        CommandProcessor processor = newProcessor();

        assertEquals("OK", processor.process("   SET    name    John   "));
        assertEquals("John", processor.process("   GET    name   "));
    }

    @Test
    void unknownCommandReturnsError() {
        CommandProcessor processor = newProcessor();

        assertEquals("ERROR unknown command", processor.process("NOPE"));
    }

    @Test
    void emptyCommandReturnsError() {
        CommandProcessor processor = newProcessor();

        assertEquals("ERROR empty command", processor.process(""));
        assertEquals("ERROR empty command", processor.process("   "));
        assertEquals("ERROR empty command", processor.process(null));
    }

    @Test
    void expireWithInvalidSecondsReturnsError() {
        CommandProcessor processor = newProcessor();

        processor.process("SET session active");

        assertEquals("ERROR seconds must be a number", processor.process("EXPIRE session abc"));
    }

    @Test
    void delExistsAndTtlWorkThroughCommands() {
        CommandProcessor processor = newProcessor();

        processor.process("SET session active");

        assertEquals("1", processor.process("EXISTS session"));
        assertEquals("-1", processor.process("TTL session"));
        assertEquals("1", processor.process("DEL session"));
        assertEquals("0", processor.process("EXISTS session"));
        assertEquals("-2", processor.process("TTL session"));
    }

    @Test
    void keysFlushAllAndRewriteAofUseTemporaryFile() {
        CommandProcessor processor = newProcessor();
        Path aofPath = tempDir.resolve("appendonly.aof");

        processor.process("SET first 1");
        processor.process("SET second 2");

        assertContainsCommandKeys(processor.process("KEYS"), "first", "second");
        assertEquals("OK", processor.process("REWRITEAOF"));
        assertTrue(Files.exists(aofPath));
        assertEquals("OK", processor.process("FLUSHALL"));
        assertEquals("(empty)", processor.process("KEYS"));
    }

    private CommandProcessor newProcessor() {
        Path aofPath = tempDir.resolve("appendonly.aof");

        return new CommandProcessor(new MiniRedis(), new AppendOnlyFile(aofPath.toString()));
    }

    private void assertContainsCommandKeys(String response, String first, String second) {
        String[] keys = response.split(" ");

        assertEquals(2, keys.length);
        assertContains(keys, first);
        assertContains(keys, second);
    }

    private void assertContains(String[] values, String expected) {
        boolean found = false;

        for (int i = 0; i < values.length; i++) {
            if (expected.equals(values[i])) {
                found = true;
            }
        }

        assertTrue(found, "Expected response to contain " + expected);
    }
}
