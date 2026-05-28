package com.joaopedro.miniredis.server;

import com.joaopedro.miniredis.command.CommandProcessor;
import com.joaopedro.miniredis.config.ServerConfig;
import com.joaopedro.miniredis.core.MiniRedis;
import com.joaopedro.miniredis.persistence.AppendOnlyFile;
import com.joaopedro.miniredis.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RedisServer
{
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    private static final Logger log = new Logger("RedisServer");

    private ServerConfig config;
    private MiniRedis redis;
    private CommandProcessor processor;
    private ExecutorService threadPool;
    private AppendOnlyFile aof;
    private ServerSocket serverSocket;
    private volatile boolean stopped;

    // Creates the Mini Redis TCP server from a validated configuration.
    // Boots the in-memory database, loads the AOF from the configured path and
    // builds the command processor with a hook that schedules stop on another
    // thread when SHUTDOWN arrives.
    public RedisServer(ServerConfig config)
    {
        this.config = config;
        this.redis = new MiniRedis();
        this.aof = new AppendOnlyFile(config.getAofPath());

        this.aof.load(redis);

        this.processor = new CommandProcessor(redis, aof, this::scheduleStop);
        this.threadPool = Executors.newFixedThreadPool(config.getMaxClients());
    }

    // Starts the TCP server.
    // Opens the ServerSocket on the configured port and accepts connections until
    // stop is called. Guarantees that the thread pool is shut down even when an
    // error happens during accept.
    public void start()
    {
        try
        {
            serverSocket = new ServerSocket(config.getPort());

            log.info("Server started on port " + config.getPort());
            log.info("AOF path: " + config.getAofPath());
            log.info("Max clients: " + config.getMaxClients());

            acceptClients(serverSocket);
        }
        catch (IOException e)
        {
            if (!stopped)
            {
                log.error("Server error: " + e.getMessage());
            }
        }
        finally
        {
            shutdownThreadPool();
        }
    }

    // Shuts the server down gracefully.
    // Marks the server as stopped, closes the ServerSocket to wake up the
    // blocking accept call and drains the thread pool. The method is idempotent
    // so it can safely be used both as a JVM shutdown hook and as the SHUTDOWN
    // command trigger.
    public void stop()
    {
        if (stopped)
        {
            return;
        }

        stopped = true;

        log.info("Shutting down Mini Redis server...");

        closeServerSocket();
        shutdownThreadPool();

        log.info("Server fully stopped");
    }

    // Schedules stop on a separate thread.
    // Used as the CommandProcessor shutdown hook so that the client that sent
    // SHUTDOWN receives the OK response before the socket is closed.
    private void scheduleStop()
    {
        Thread stopper = new Thread(this::stop, "mini-redis-shutdown");
        stopper.setDaemon(true);
        stopper.start();
    }

    // Accepts clients connected to the server.
    // Creates a ClientHandler for every accepted socket and dispatches it to the
    // thread pool. Ignores SocketException when the server is already stopping
    // because that is expected when the ServerSocket is closed during shutdown.
    private void acceptClients(ServerSocket serverSocket) throws IOException
    {
        while (!stopped)
        {
            try
            {
                Socket clientSocket = serverSocket.accept();

                log.info("Client connected: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, processor);

                threadPool.execute(handler);
            }
            catch (SocketException e)
            {
                if (!stopped)
                {
                    throw e;
                }
            }
        }
    }

    // Closes the server's ServerSocket.
    // Handles IOException locally to make sure that a failure while closing the
    // socket does not break the shutdown flow.
    private void closeServerSocket()
    {
        if (serverSocket != null)
        {
            try
            {
                serverSocket.close();
            }
            catch (IOException e)
            {
                log.warn("Error closing server socket: " + e.getMessage());
            }
        }
    }

    // Shuts down the server thread pool.
    // Signals shutdown, waits up to the configured timeout for the active client
    // handlers to finish and falls back to shutdownNow if they do not terminate
    // in time. The timeout protects against idle clients that would never finish.
    private void shutdownThreadPool()
    {
        if (threadPool == null)
        {
            return;
        }

        if (threadPool.isShutdown())
        {
            return;
        }

        threadPool.shutdown();

        try
        {
            if (!threadPool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS))
            {
                threadPool.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
