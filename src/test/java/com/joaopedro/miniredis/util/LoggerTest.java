package com.joaopedro.miniredis.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerTest
{
    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream capturedOut;
    private ByteArrayOutputStream capturedErr;

    @BeforeEach
    void redirectStreams()
    {
        originalOut = System.out;
        originalErr = System.err;

        capturedOut = new ByteArrayOutputStream();
        capturedErr = new ByteArrayOutputStream();

        System.setOut(new PrintStream(capturedOut));
        System.setErr(new PrintStream(capturedErr));
    }

    @AfterEach
    void restoreStreams()
    {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void infoGoesToStdoutWithLevelAndComponent()
    {
        Logger log = new Logger("TestComponent");

        log.info("server up");

        String output = capturedOut.toString();

        assertTrue(output.contains("[INFO "), "expected level marker, got: " + output);
        assertTrue(output.contains("[TestComponent]"), "expected component, got: " + output);
        assertTrue(output.contains("server up"), "expected message, got: " + output);
        assertTrue(capturedErr.toString().isEmpty(), "INFO should not go to stderr");
    }

    @Test
    void warnGoesToStderr()
    {
        Logger log = new Logger("TestComponent");

        log.warn("slow disk");

        String errOutput = capturedErr.toString();

        assertTrue(errOutput.contains("[WARN "), "expected WARN marker, got: " + errOutput);
        assertTrue(errOutput.contains("slow disk"));
        assertTrue(capturedOut.toString().isEmpty(), "WARN should not go to stdout");
    }

    @Test
    void errorGoesToStderr()
    {
        Logger log = new Logger("TestComponent");

        log.error("disk full");

        String errOutput = capturedErr.toString();

        assertTrue(errOutput.contains("[ERROR]"), "expected ERROR marker, got: " + errOutput);
        assertTrue(errOutput.contains("disk full"));
        assertTrue(capturedOut.toString().isEmpty(), "ERROR should not go to stdout");
    }

    @Test
    void outputContainsTimestamp()
    {
        Logger log = new Logger("TestComponent");

        log.info("hello");

        String output = capturedOut.toString();

        // Timestamp expected at the start in the format yyyy-MM-dd HH:mm:ss.
        assertTrue(output.matches("(?s)\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\].*"),
                "expected timestamp at start, got: " + output);
    }
}
