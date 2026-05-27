package com.joaopedro.miniredis.server;

import com.joaopedro.miniredis.command.CommandProcessor;
import com.joaopedro.miniredis.core.MiniRedis;
import com.joaopedro.miniredis.persistence.AppendOnlyFile;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisServer {
    private static final int MAX_CLIENTS = 10;

    private int port;
    private MiniRedis redis;
    private CommandProcessor processor;
    private ExecutorService threadPool;
    private AppendOnlyFile aof;

    // Cria o servidor TCP do Mini Redis.
    // Recebe a porta, cria o banco, carrega o AOF e cria o processador de comandos
    // compartilhado.
    public RedisServer(int port) {
        this.port = port;
        this.redis = new MiniRedis();
        this.aof = new AppendOnlyFile("data/appendonly.aof");

        this.aof.load(redis);

        this.processor = new CommandProcessor(redis, aof);
        this.threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
    }

    // Inicia o servidor TCP.
    // Abre um ServerSocket na porta configurada e fica aceitando conexoes de
    // clientes.
    public void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            System.out.println("Mini Redis server started on port " + port);
            System.out.println("Max clients: " + MAX_CLIENTS);

            acceptClients(serverSocket);
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        } finally {
            shutdownThreadPool();
        }
    }

    // Aceita clientes conectados ao servidor.
    // Para cada cliente, cria um ClientHandler e envia esse trabalho para o thread
    // pool.
    private void acceptClients(ServerSocket serverSocket) throws IOException {
        while (true) {
            Socket clientSocket = serverSocket.accept();

            System.out.println("Client connected: " + clientSocket.getInetAddress());

            ClientHandler handler = new ClientHandler(clientSocket, processor);

            threadPool.execute(handler);
        }
    }

    // Encerra o pool de threads do servidor.
    // Esse metodo e chamado quando o servidor termina ou ocorre algum erro critico.
    private void shutdownThreadPool() {
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }
}