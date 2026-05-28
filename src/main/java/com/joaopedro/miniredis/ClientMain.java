package com.joaopedro.miniredis;

import com.joaopedro.miniredis.client.MiniRedisClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ClientMain
{
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 6379;

    // Starts the interactive Mini Redis client.
    // Resolves host and port from the arguments, connects to the server and
    // enters the terminal read loop. Closes the connection on exit.
    public static void main(String[] args)
    {
        String host = resolveHost(args);
        int port = resolvePort(args);

        MiniRedisClient client = new MiniRedisClient(host, port);

        try
        {
            String[] welcome = client.connect();

            printWelcome(host, port, welcome);

            runLoop(client);
        }
        catch (IOException e)
        {
            System.out.println("Could not connect to " + host + ":" + port + " - " + e.getMessage());
        }
        finally
        {
            client.close();
        }
    }

    // Reads the host from the command-line arguments.
    // Uses the first argument when provided, otherwise returns the default host
    // "localhost".
    private static String resolveHost(String[] args)
    {
        String result = DEFAULT_HOST;

        if (args.length >= 1)
        {
            result = args[0];
        }

        return result;
    }

    // Reads the port from the command-line arguments.
    // Uses the second argument when it exists and is a valid number, otherwise
    // returns the default port 6379.
    private static int resolvePort(String[] args)
    {
        int result = DEFAULT_PORT;

        if (args.length >= 2)
        {
            try
            {
                result = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException e)
            {
                System.out.println("Invalid port '" + args[1] + "', using default " + DEFAULT_PORT);
            }
        }

        return result;
    }

    // Prints the initial information for the user.
    // Shows the connected address and the welcome lines returned by the server.
    private static void printWelcome(String host, int port, String[] welcome)
    {
        System.out.println("Connected to " + host + ":" + port);

        for (int i = 0; i < welcome.length; i++)
        {
            if (welcome[i] != null)
            {
                System.out.println(welcome[i]);
            }
        }
    }

    // Reads commands from the terminal and forwards them to the server in a loop.
    // Stops when the user types QUIT, when stdin reaches EOF or when an I/O
    // error breaks the connection.
    private static void runLoop(MiniRedisClient client) throws IOException
    {
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("> ");

        String line = stdin.readLine();

        while (line != null)
        {
            if (client.isQuitCommand(line))
            {
                String bye = client.sendCommand(line);

                if (bye != null)
                {
                    System.out.println(bye);
                }

                line = null;
            }
            else
            {
                String response = client.sendCommand(line);

                if (response == null)
                {
                    System.out.println("Server closed the connection");
                    line = null;
                }
                else
                {
                    System.out.println(response);

                    System.out.print("> ");

                    line = stdin.readLine();
                }
            }
        }
    }
}
