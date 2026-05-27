package com.joaopedro.miniredis.server;

import com.joaopedro.miniredis.command.CommandProcessor;
import com.joaopedro.miniredis.core.MiniRedis;
import com.joaopedro.miniredis.persistence.AppendOnlyFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientHandlerTest {

    @TempDir
    Path tempDir;

    @Test
    void quitWithExtraSpacesDisconnectsTcpClient() throws Exception {
        ServerSocket serverSocket = new ServerSocket(0);
        Thread serverThread = new Thread(() -> acceptOneClient(serverSocket));
        serverThread.start();

        Socket clientSocket = new Socket("localhost", serverSocket.getLocalPort());
        clientSocket.setSoTimeout(2000);

        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

        assertEquals("Mini Redis Java connected", reader.readLine());
        assertEquals("Type commands or QUIT to disconnect", reader.readLine());

        writer.println("   QUIT   ");

        assertEquals("Bye", reader.readLine());

        clientSocket.close();
        serverThread.join(2000);
        serverSocket.close();
    }

    private void acceptOneClient(ServerSocket serverSocket) {
        try {
            Socket socket = serverSocket.accept();
            ClientHandler handler = new ClientHandler(socket, newProcessor());

            handler.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CommandProcessor newProcessor() {
        Path aofPath = tempDir.resolve("appendonly.aof");

        return new CommandProcessor(new MiniRedis(), new AppendOnlyFile(aofPath.toString()));
    }
}
