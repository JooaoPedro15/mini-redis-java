package com.joaopedro.miniredis.command;

import com.joaopedro.miniredis.core.MiniRedis;
import com.joaopedro.miniredis.persistence.AppendOnlyFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

        assertEquals("OK", processor.process("SET nome joao pedro costa"));
        assertEquals("joao pedro costa", processor.process("GET nome"));
    }

    @Test
    void commandsAcceptExtraSpaces() {
        CommandProcessor processor = newProcessor();

        assertEquals("OK", processor.process("   SET    name    John   "));
        assertEquals("John", processor.process("   GET    name   "));
    }

    @Test
    void allFinalCommandsWorkThroughProcessor() {
        CommandProcessor processor = newProcessor();

        assertEquals("PONG", processor.process("PING"));
        assertEquals("OK", processor.process("SET name John"));
        assertEquals("John", processor.process("GET name"));
        assertEquals("1", processor.process("EXISTS name"));
        assertEquals("1", processor.process("EXPIRE name 10"));
        assertTrue(isPositiveNumber(processor.process("TTL name")));
        assertContainsCommandKeys(processor.process("KEYS"), "name");
        assertEquals("OK", processor.process("REWRITEAOF"));
        assertEquals("1", processor.process("DEL name"));
        assertEquals("OK", processor.process("FLUSHALL"));
        assertEquals("(empty)", processor.process("KEYS"));
    }

    @Test
    void missingArgumentsReturnUsageErrors() {
        CommandProcessor processor = newProcessor();

        assertEquals("ERROR usage: SET key value", processor.process("SET"));
        assertEquals("ERROR usage: SET key value", processor.process("SET name"));
        assertEquals("ERROR usage: GET key", processor.process("GET"));
        assertEquals("ERROR usage: DEL key", processor.process("DEL"));
        assertEquals("ERROR usage: EXISTS key", processor.process("EXISTS"));
        assertEquals("ERROR usage: EXPIRE key seconds", processor.process("EXPIRE name"));
        assertEquals("ERROR usage: TTL key", processor.process("TTL"));
        assertEquals("ERROR usage: KEYS", processor.process("KEYS extra"));
        assertEquals("ERROR usage: FLUSHALL", processor.process("FLUSHALL extra"));
        assertEquals("ERROR usage: REWRITEAOF", processor.process("REWRITEAOF extra"));
        assertEquals("ERROR usage: PING", processor.process("PING extra"));
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
    void mutatingCommandsKeepWritingToTemporaryAof() throws Exception {
        CommandProcessor processor = newProcessor();
        Path aofPath = tempDir.resolve("appendonly.aof");

        processor.process("SET nome joao pedro costa");
        processor.process("EXPIRE nome 60");
        processor.process("DEL nome");
        processor.process("FLUSHALL");

        List<String> lines = Files.readAllLines(aofPath);

        assertTrue(lines.contains("SET nome joao pedro costa"));
        assertTrue(containsLineStartingWith(lines, "EXPIREAT nome "));
        assertTrue(lines.contains("DEL nome"));
        assertTrue(lines.contains("FLUSHALL"));
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
    void shutdownWithoutHookReturnsError() {
        CommandProcessor processor = newProcessor();

        assertEquals("ERROR SHUTDOWN not available", processor.process("SHUTDOWN"));
    }

    @Test
    void shutdownWithHookReturnsOkAndTriggersHook() {
        AtomicBoolean triggered = new AtomicBoolean(false);
        Path aofPath = tempDir.resolve("appendonly.aof");

        CommandProcessor processor = new CommandProcessor(
                new MiniRedis(),
                new AppendOnlyFile(aofPath.toString()),
                () -> triggered.set(true));

        assertEquals("OK", processor.process("SHUTDOWN"));
        assertTrue(triggered.get(), "shutdown hook should have been triggered");
    }

    @Test
    void shutdownWithExtraArgsReturnsUsageError() {
        CommandProcessor processor = newProcessor();

        assertEquals("ERROR usage: SHUTDOWN", processor.process("SHUTDOWN extra"));
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

    private void assertContainsCommandKeys(String response, String expected) {
        String[] keys = response.split(" ");

        assertContains(keys, expected);
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

    private boolean isPositiveNumber(String value) {
        boolean result = false;

        try {
            long number = Long.parseLong(value);

            if (number > 0) {
                result = true;
            }
        } catch (NumberFormatException e) {
            result = false;
        }

        return result;
    }

    private boolean containsLineStartingWith(List<String> lines, String expectedStart) {
        boolean result = false;

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(expectedStart)) {
                result = true;
            }
        }

        return result;
    }
}
