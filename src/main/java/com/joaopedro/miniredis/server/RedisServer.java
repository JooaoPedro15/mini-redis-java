package com.joaopedro.miniredis.server;

import com.joaopedro.miniredis.command.CommandProcessor;
import com.joaopedro.miniredis.core.MiniRedis;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class RedisServer
{
    private int port;
    private MiniRedis redis;
    private CommandProcessor processor;

    // Cria o servidor TCP do Mini Redis.
    // Recebe a porta, cria o banco em memoria e cria o processador de comandos compartilhado.
    public RedisServer(int port)
    {
        this.port = port;
        this.redis = new MiniRedis();
        this.processor = new CommandProcessor(redis);
    }

    // Inicia o servidor TCP.
    // Abre um ServerSocket na porta configurada e fica aceitando clientes em loop.
    public void start()
    {
        try
        {
            ServerSocket serverSocket = new ServerSocket(port);

            System.out.println("Mini Redis server started on port " + port);

            acceptClients(serverSocket);
        }
        catch (IOException e)
        {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    // Aceita clientes conectados ao servidor.
    // Para cada cliente, cria um ClientHandler e inicia uma nova thread para atender a conexao.
    private void acceptClients(ServerSocket serverSocket) throws IOException
    {
        while (true)
        {
            Socket clientSocket = serverSocket.accept();

            System.out.println("Client connected: " + clientSocket.getInetAddress());

            ClientHandler handler = new ClientHandler(clientSocket, processor);
            Thread thread = new Thread(handler);

            thread.start();
        }
    }
}