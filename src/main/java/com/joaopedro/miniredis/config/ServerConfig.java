package com.joaopedro.miniredis.config;

public class ServerConfig
{
    public static final int DEFAULT_PORT = 6379;
    public static final String DEFAULT_AOF_PATH = "data/appendonly.aof";
    public static final int DEFAULT_MAX_CLIENTS = 10;

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    private int port;
    private String aofPath;
    private int maxClients;

    // Creates a validated server configuration.
    // Each field is validated at construction time so the object only exists in
    // a valid state. Failures become IllegalArgumentException with a clear message.
    public ServerConfig(int port, String aofPath, int maxClients)
    {
        if (port < MIN_PORT || port > MAX_PORT)
        {
            throw new IllegalArgumentException(
                    "port must be between " + MIN_PORT + " and " + MAX_PORT + ", got: " + port);
        }

        if (aofPath == null || aofPath.trim().isEmpty())
        {
            throw new IllegalArgumentException("aofPath must not be empty");
        }

        if (maxClients < 1)
        {
            throw new IllegalArgumentException("maxClients must be greater than 0, got: " + maxClients);
        }

        this.port = port;
        this.aofPath = aofPath;
        this.maxClients = maxClients;
    }

    // Returns the TCP port the server should listen on.
    public int getPort()
    {
        return port;
    }

    // Returns the path of the AOF file used for persistence.
    public String getAofPath()
    {
        return aofPath;
    }

    // Returns the maximum number of concurrent clients served by the thread pool.
    public int getMaxClients()
    {
        return maxClients;
    }

    // Builds a ServerConfig using every default value.
    // Convenient shortcut for tests and callers that do not need any customization.
    public static ServerConfig defaults()
    {
        return new ServerConfig(DEFAULT_PORT, DEFAULT_AOF_PATH, DEFAULT_MAX_CLIENTS);
    }

    // Parses the command-line arguments.
    // Accepts the flags --port, --aof and --max-clients, each followed by its
    // value. Any unknown argument, missing value or invalid value throws an
    // IllegalArgumentException. Flags that are not supplied keep their defaults.
    public static ServerConfig parse(String[] args)
    {
        int port = DEFAULT_PORT;
        String aofPath = DEFAULT_AOF_PATH;
        int maxClients = DEFAULT_MAX_CLIENTS;

        int i = 0;

        while (i < args.length)
        {
            String arg = args[i];

            if (arg.equals("--port"))
            {
                port = readIntValue(args, i, "--port");
                i = i + 2;
            }
            else if (arg.equals("--aof"))
            {
                aofPath = readStringValue(args, i, "--aof");
                i = i + 2;
            }
            else if (arg.equals("--max-clients"))
            {
                maxClients = readIntValue(args, i, "--max-clients");
                i = i + 2;
            }
            else
            {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        return new ServerConfig(port, aofPath, maxClients);
    }

    // Reads the next argument as an integer.
    // Verifies that a next argument exists and that it is numeric. Error messages
    // include the flag name to make diagnostics easier.
    private static int readIntValue(String[] args, int index, String name)
    {
        if (index + 1 >= args.length)
        {
            throw new IllegalArgumentException(name + " requires a value");
        }

        String raw = args[index + 1];

        try
        {
            return Integer.parseInt(raw);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException(name + " requires an integer, got: " + raw);
        }
    }

    // Reads the next argument as a String.
    // Only verifies that a next argument exists. The content validation (for
    // example, non-empty String) is performed by the constructor.
    private static String readStringValue(String[] args, int index, String name)
    {
        if (index + 1 >= args.length)
        {
            throw new IllegalArgumentException(name + " requires a value");
        }

        return args[index + 1];
    }

    // Returns the usage text of the server.
    // Centralizes flag documentation in a single place so Main can print this
    // text whenever parsing fails.
    public static String usage()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("Usage: java -cp target\\classes com.joaopedro.miniredis.Main [options]").append(System.lineSeparator());
        builder.append("Options:").append(System.lineSeparator());
        builder.append("  --port <number>       TCP port (")
                .append(MIN_PORT).append("-").append(MAX_PORT)
                .append(", default ").append(DEFAULT_PORT).append(")").append(System.lineSeparator());
        builder.append("  --aof <path>          AOF file path (default ").append(DEFAULT_AOF_PATH).append(")").append(System.lineSeparator());
        builder.append("  --max-clients <n>     Max concurrent clients (>0, default ").append(DEFAULT_MAX_CLIENTS).append(")");

        return builder.toString();
    }
}
