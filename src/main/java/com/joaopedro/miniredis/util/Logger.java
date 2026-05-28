package com.joaopedro.miniredis.util;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger
{
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String component;

    // Creates a logger bound to a component name.
    // The component name appears in every log line to make it easier to identify
    // the source of a message when multiple modules are logging.
    public Logger(String component)
    {
        this.component = component;
    }

    // Logs an INFO message.
    // Writes to stdout because INFO represents normal server events and does not
    // need to be separated from regular output.
    public void info(String message)
    {
        log("INFO ", message, System.out);
    }

    // Logs a WARN message.
    // Writes to stderr so warnings can be filtered or redirected independently
    // from normal output.
    public void warn(String message)
    {
        log("WARN ", message, System.err);
    }

    // Logs an ERROR message.
    // Writes to stderr because it indicates a failure. Keeps the same format as
    // the other levels for consistency.
    public void error(String message)
    {
        log("ERROR", message, System.err);
    }

    // Formats and prints a single log line.
    // Builds the line with timestamp, level, component and message and sends it
    // to the given PrintStream. PrintStream.println is internally synchronized,
    // so concurrent callers do not interleave inside a single line.
    private void log(String level, String message, PrintStream out)
    {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        out.println("[" + timestamp + "] [" + level + "] [" + component + "] " + message);
    }
}
