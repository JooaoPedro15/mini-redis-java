package com.joaopedro.miniredis;

import com.joaopedro.miniredis.client.MiniRedisClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ClientMain
{
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 6379;

    // Inicia o cliente interativo do Mini Redis.
    // Resolve host e porta a partir dos argumentos, conecta no servidor e entra no loop de leitura do terminal.
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

    // Le o host a partir dos argumentos da linha de comando.
    // Usa o primeiro argumento se existir, caso contrario retorna o host padrao localhost.
    private static String resolveHost(String[] args)
    {
        String result = DEFAULT_HOST;

        if (args.length >= 1)
        {
            result = args[0];
        }

        return result;
    }

    // Le a porta a partir dos argumentos da linha de comando.
    // Usa o segundo argumento se existir e for um numero valido, caso contrario retorna a porta padrao 6379.
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

    // Imprime informacoes iniciais para o usuario.
    // Mostra o endereco conectado e as linhas de boas-vindas devolvidas pelo servidor.
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

    // Le comandos do terminal e envia para o servidor em loop.
    // Encerra quando o usuario digita QUIT, quando a entrada termina (EOF) ou quando ocorre um erro de IO.
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
