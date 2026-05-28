package com.joaopedro.miniredis;

import com.joaopedro.miniredis.config.ServerConfig;
import com.joaopedro.miniredis.server.RedisServer;

public class Main
{
    // Starts the Mini Redis TCP server.
    // Parses the command-line arguments into a ServerConfig, creates the
    // RedisServer with that configuration, registers a JVM shutdown hook so
    // Ctrl+C also stops the server gracefully and calls start to accept
    // connections. When the arguments are invalid, prints the error plus the
    // usage text to stderr and exits with code 2.
    public static void main(String[] args)
    {
        ServerConfig config;

        try
        {
            config = ServerConfig.parse(args);
        }
        catch (IllegalArgumentException e)
        {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println(ServerConfig.usage());

            System.exit(2);

            return;
        }

        RedisServer server = new RedisServer(config);

        Thread jvmShutdownHook = new Thread(server::stop, "mini-redis-jvm-shutdown");

        Runtime.getRuntime().addShutdownHook(jvmShutdownHook);

        server.start();
    }
}
