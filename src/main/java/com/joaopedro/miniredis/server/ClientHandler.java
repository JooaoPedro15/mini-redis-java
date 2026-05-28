package com.joaopedro.miniredis.server;

import com.joaopedro.miniredis.command.CommandProcessor;
import com.joaopedro.miniredis.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable
{
    private static final Logger log = new Logger("ClientHandler");

    private Socket clientSocket;
    private CommandProcessor processor;

    // Creates the handler for a single TCP client.
    // Stores the client socket and the shared command processor used to execute
    // every command received from this client.
    public ClientHandler(Socket clientSocket, CommandProcessor processor)
    {
        this.clientSocket = clientSocket;
        this.processor = processor;
    }

    // Runs the client session on its own thread.
    // Delegates to handleClient, logs the disconnection in the finally block and
    // guarantees that the socket is closed even when an exception is raised.
    @Override
    public void run()
    {
        String clientAddress = String.valueOf(clientSocket.getInetAddress());

        try
        {
            handleClient();
        }
        catch (IOException e)
        {
            log.warn("Client connection error (" + clientAddress + "): " + e.getMessage());
        }
        finally
        {
            closeClientSocket();
            log.info("Client disconnected: " + clientAddress);
        }
    }

    // Reads commands from the client and writes back responses.
    // For every line received, runs it through the CommandProcessor and writes
    // the result on the same socket. Stops the loop when QUIT is received or the
    // remote end closes the connection.
    private void handleClient() throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

        writer.println("Mini Redis Java connected");
        writer.println("Type commands or QUIT to disconnect");

        String line = reader.readLine();

        while (line != null && !isQuitCommand(line))
        {
            String response = processor.process(line);

            writer.println(response);

            line = reader.readLine();
        }

        writer.println("Bye");
    }

    // Checks whether a received line is the QUIT command.
    // Trims leading and trailing whitespace before comparing so QUIT can end the
    // session even when surrounded by extra spaces.
    private boolean isQuitCommand(String line)
    {
        boolean result = false;

        if (line != null && line.trim().equalsIgnoreCase("QUIT"))
        {
            result = true;
        }

        return result;
    }

    // Closes the client connection.
    // Wraps the close call in try/catch so a failure while closing the socket
    // does not crash the server.
    private void closeClientSocket()
    {
        try
        {
            clientSocket.close();
        }
        catch (IOException e)
        {
            log.warn("Error closing client socket: " + e.getMessage());
        }
    }
}
