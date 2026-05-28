package com.joaopedro.miniredis.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MiniRedisClient
{
    private String host;
    private int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    // Creates a Mini Redis TCP client.
    // Stores host and port only. The actual connection is opened by connect.
    public MiniRedisClient(String host, int port)
    {
        this.host = host;
        this.port = port;
    }

    // Opens the TCP connection to the server.
    // Creates the socket, sets up reader and writer, and reads the welcome lines
    // sent by the server. Returns the welcome lines so the caller can display them.
    public String[] connect() throws IOException
    {
        this.socket = new Socket(host, port);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);

        String firstLine = reader.readLine();
        String secondLine = reader.readLine();

        return new String[] { firstLine, secondLine };
    }

    // Sends a command to the server and returns the response.
    // Writes the line on the socket and reads exactly one response line, since
    // the server protocol always answers in a single line.
    public String sendCommand(String command) throws IOException
    {
        writer.println(command);

        return reader.readLine();
    }

    // Checks whether the line typed by the user is the QUIT command.
    // Ignores surrounding whitespace and case differences.
    public boolean isQuitCommand(String line)
    {
        boolean result = false;

        if (line != null && line.trim().equalsIgnoreCase("QUIT"))
        {
            result = true;
        }

        return result;
    }

    // Closes the connection with the server.
    // Wraps the close call in try/catch so a failure while closing does not
    // break the calling application.
    public void close()
    {
        try
        {
            if (socket != null)
            {
                socket.close();
            }
        }
        catch (IOException e)
        {
            System.out.println("Error closing client socket: " + e.getMessage());
        }
    }
}
