package com.joaopedro.miniredis.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RedisServerTest {

    @TempDir
    Path tempDir;

    @Test
    void shutdownCommandStopsServerGracefully() throws Exception {
        int port = findFreePort();
        String aofPath = tempDir.resolve("appendonly.aof").toString();

        RedisServer server = new RedisServer(port, aofPath);

        Thread serverThread = new Thread(server::start, "test-server");
        serverThread.start();

        waitForPortOpen(port, 2000);

        Socket client = new Socket("localhost", port);
        client.setSoTimeout(2000);

        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter writer = new PrintWriter(client.getOutputStream(), true);

        assertEquals("Mini Redis Java connected", reader.readLine());
        assertEquals("Type commands or QUIT to disconnect", reader.readLine());

        writer.println("SHUTDOWN");
        assertEquals("OK", reader.readLine());

        client.close();

        serverThread.join(5000);
        assertFalse(serverThread.isAlive(), "server thread should have stopped after SHUTDOWN");

        assertThrows(ConnectException.class, () -> {
            Socket reconnect = new Socket("localhost", port);
            reconnect.close();
        });
    }

    @Test
    void stopIsIdempotent() throws Exception {
        int port = findFreePort();
        String aofPath = tempDir.resolve("appendonly.aof").toString();

        RedisServer server = new RedisServer(port, aofPath);

        Thread serverThread = new Thread(server::start, "test-server-idempotent");
        serverThread.start();

        waitForPortOpen(port, 2000);

        server.stop();
        server.stop();

        serverThread.join(5000);
        assertFalse(serverThread.isAlive(), "server thread should have stopped");
    }

    private int findFreePort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();

        socket.close();

        return port;
    }

    private void waitForPortOpen(int port, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            try {
                Socket probe = new Socket("localhost", port);
                probe.close();
                return;
            } catch (IOException e) {
                Thread.sleep(50);
            }
        }
    }
}
