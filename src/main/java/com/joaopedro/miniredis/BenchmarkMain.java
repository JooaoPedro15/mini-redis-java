package com.joaopedro.miniredis;

import com.joaopedro.miniredis.core.MiniRedis;

import java.util.Locale;

// Simple educational benchmark for MiniRedis.
//
// IMPORTANT: this class is NOT a replacement for tools like JMH. The goal is
// to provide a rough measurement that helps compare project versions among
// themselves and to get an order-of-magnitude feel for the basic operations.
// The numbers depend heavily on the machine, the JVM, the system load and
// even the processor temperature.
//
// The benchmark exercises the in-memory core of the database, without TCP,
// without AOF and without the command parser. The focus is the manual hash
// table and the Entry logic.
public class BenchmarkMain
{
    private static final int DEFAULT_OPERATIONS = 100_000;
    private static final int WARMUP_OPERATIONS = 10_000;

    // Sink used to prevent the JIT from eliminating calls without a visible effect.
    // It is volatile and printed at the end so the return values of the operations
    // are considered "used" by the optimizer.
    private static volatile long sink = 0;

    // Benchmark entry point.
    // Resolves the number of operations from the arguments, prints the header,
    // runs the warmup and executes the 4 scenarios in sequence.
    public static void main(String[] args)
    {
        int operations = resolveOperations(args);

        printHeader(operations);
        warmup();

        runSetBenchmark(operations);
        runGetBenchmark(operations);
        runDelBenchmark(operations);
        runMixBenchmark(operations);

        printFooter();
    }

    // Reads the number of operations from the command-line arguments.
    // Uses the first argument when it is a positive integer, otherwise falls
    // back to the default and prints a warning.
    private static int resolveOperations(String[] args)
    {
        int result = DEFAULT_OPERATIONS;

        if (args.length >= 1)
        {
            try
            {
                int parsed = Integer.parseInt(args[0]);

                if (parsed > 0)
                {
                    result = parsed;
                }
                else
                {
                    System.out.println("Operations must be positive, using default " + DEFAULT_OPERATIONS);
                }
            }
            catch (NumberFormatException e)
            {
                System.out.println("Invalid number '" + args[0] + "', using default " + DEFAULT_OPERATIONS);
            }
        }

        return result;
    }

    // Runs a JIT warmup before the real measurements.
    // Performs SET followed by GET on a throwaway instance so the JIT compiler
    // optimizes the hot paths before the measured scenarios start.
    private static void warmup()
    {
        System.out.println("Warmup: " + WARMUP_OPERATIONS + " operations...");

        MiniRedis redis = new MiniRedis();

        for (int i = 0; i < WARMUP_OPERATIONS; i++)
        {
            redis.set("warm" + i, "v");
        }

        for (int i = 0; i < WARMUP_OPERATIONS; i++)
        {
            String value = redis.get("warm" + i);

            if (value != null)
            {
                sink = sink + value.hashCode();
            }
        }

        System.out.println("Warmup done.");
        System.out.println();
    }

    // Measures the time of N SET operations against an empty database.
    // Pre-generates the keys so string concatenation does not pollute the
    // measurement window.
    private static void runSetBenchmark(int operations)
    {
        MiniRedis redis = new MiniRedis();
        String[] keys = generateKeys(operations);

        long start = System.nanoTime();

        for (int i = 0; i < operations; i++)
        {
            String result = redis.set(keys[i], "value");

            sink = sink + result.hashCode();
        }

        long elapsed = System.nanoTime() - start;

        printResult("SET", operations, elapsed);
    }

    // Measures the time of N GET operations against a pre-populated database.
    // Populates the database outside the measurement window so only the GET time
    // is counted.
    private static void runGetBenchmark(int operations)
    {
        MiniRedis redis = new MiniRedis();
        String[] keys = generateKeys(operations);

        for (int i = 0; i < operations; i++)
        {
            redis.set(keys[i], "value");
        }

        long start = System.nanoTime();

        for (int i = 0; i < operations; i++)
        {
            String value = redis.get(keys[i]);

            if (value != null)
            {
                sink = sink + value.hashCode();
            }
        }

        long elapsed = System.nanoTime() - start;

        printResult("GET", operations, elapsed);
    }

    // Measures the time of N DEL operations against a pre-populated database.
    // Every key exists at the start of the loop, so every removal hits a real
    // value and follows the actual removal path of the hash table.
    private static void runDelBenchmark(int operations)
    {
        MiniRedis redis = new MiniRedis();
        String[] keys = generateKeys(operations);

        for (int i = 0; i < operations; i++)
        {
            redis.set(keys[i], "value");
        }

        long start = System.nanoTime();

        for (int i = 0; i < operations; i++)
        {
            int removed = redis.del(keys[i]);

            sink = sink + removed;
        }

        long elapsed = System.nanoTime() - start;

        printResult("DEL", operations, elapsed);
    }

    // Measures the time of a 50/50 mixed workload of SET and GET.
    // Even indices SET a fresh key, odd indices GET the key written on the
    // previous iteration, so half of the GETs are hits.
    private static void runMixBenchmark(int operations)
    {
        MiniRedis redis = new MiniRedis();
        String[] keys = generateKeys(operations);

        long start = System.nanoTime();

        for (int i = 0; i < operations; i++)
        {
            if (i % 2 == 0)
            {
                String result = redis.set(keys[i], "value");

                sink = sink + result.hashCode();
            }
            else
            {
                String value = redis.get(keys[i - 1]);

                if (value != null)
                {
                    sink = sink + value.hashCode();
                }
            }
        }

        long elapsed = System.nanoTime() - start;

        printResult("MIX 50/50 SET+GET", operations, elapsed);
    }

    // Generates an array of keys with the format "k<i>".
    // Performs the string-building work outside the benchmark window so
    // allocation and concatenation do not pollute the measurement.
    private static String[] generateKeys(int count)
    {
        String[] keys = new String[count];

        for (int i = 0; i < count; i++)
        {
            keys[i] = "k" + i;
        }

        return keys;
    }

    // Prints the result of a scenario.
    // Converts nanoseconds to milliseconds and computes operations per second
    // as ops divided by elapsed seconds. Forces Locale.ROOT in the format call
    // to guarantee a dot decimal separator regardless of the system locale.
    private static void printResult(String label, int operations, long elapsedNanos)
    {
        double millis = elapsedNanos / 1_000_000.0;
        double seconds = elapsedNanos / 1_000_000_000.0;
        double opsPerSecond = operations / seconds;

        System.out.println(String.format(
                Locale.ROOT,
                "[%s] %d ops in %.2f ms = %.0f ops/sec",
                label, operations, millis, opsPerSecond));
    }

    // Prints the benchmark header.
    // Always shows the warning that this measurement is not a replacement for
    // tools like JMH, reinforcing that the numbers are only approximate.
    private static void printHeader(int operations)
    {
        System.out.println("========================================================");
        System.out.println("Mini Redis - Simple Benchmark");
        System.out.println("========================================================");
        System.out.println("This is NOT JMH. Numbers are rough and machine-dependent.");
        System.out.println("Use for relative comparison, not as a definitive measure.");
        System.out.println("--------------------------------------------------------");
        System.out.println("Operations per scenario: " + operations);
        System.out.println("Warmup operations:       " + WARMUP_OPERATIONS);
        System.out.println("========================================================");
        System.out.println();
    }

    // Prints the benchmark footer.
    // Also prints the sink to make sure the JIT does not remove the work done
    // inside the loops by treating it as having no observable effect.
    private static void printFooter()
    {
        System.out.println();
        System.out.println("========================================================");
        System.out.println("Done. sink=" + sink);
        System.out.println("========================================================");
    }
}
