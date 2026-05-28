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

public class RedisServer {
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    private static final Logger log = new Logger("RedisServer");

    private ServerConfig config;
    private MiniRedis redis;
    private CommandProcessor processor;
    private ExecutorService threadPool;
    private AppendOnlyFile aof;
    private ServerSocket serverSocket;
    private volatile boolean stopped;

    // Cria o servidor TCP do Mini Redis a partir de uma configuracao validada.
    // Inicializa o banco em memoria, carrega o AOF do caminho configurado e monta
    // o processador de comandos com hook que dispara stop em outra thread.
    public RedisServer(ServerConfig config) {
        this.config = config;
        this.redis = new MiniRedis();
        this.aof = new AppendOnlyFile(config.getAofPath());

        this.aof.load(redis);

        this.processor = new CommandProcessor(redis, aof, this::scheduleStop);
        this.threadPool = Executors.newFixedThreadPool(config.getMaxClients());
    }

    // Inicia o servidor TCP.
    // Abre o ServerSocket na porta configurada e aceita conexoes ate stop ser
    // chamado. Garante que o thread pool seja encerrado mesmo em caso de erro.
    public void start() {
        try {
            serverSocket = new ServerSocket(config.getPort());

            log.info("Server started on port " + config.getPort());
            log.info("AOF path: " + config.getAofPath());
            log.info("Max clients: " + config.getMaxClients());

            acceptClients(serverSocket);
        } catch (IOException e) {
            if (!stopped) {
                log.error("Server error: " + e.getMessage());
            }
        } finally {
            shutdownThreadPool();
        }
    }

    // Encerra o servidor de forma controlada.
    // Marca o servidor como parado, fecha o ServerSocket para acordar o accept
    // bloqueado e desliga o thread pool. E idempotente: chamadas adicionais sao
    // ignoradas, o que permite usar este metodo como JVM shutdown hook e como
    // gatilho do comando SHUTDOWN sem efeito colateral.
    public void stop() {
        if (stopped) {
            return;
        }

        stopped = true;

        log.info("Shutting down Mini Redis server...");

        closeServerSocket();
        shutdownThreadPool();

        log.info("Server fully stopped");
    }

    // Agenda o stop em uma thread separada.
    // Usado como shutdown hook do CommandProcessor para que o cliente que enviou
    // SHUTDOWN receba a resposta OK antes do socket ser fechado.
    private void scheduleStop() {
        Thread stopper = new Thread(this::stop, "mini-redis-shutdown");
        stopper.setDaemon(true);
        stopper.start();
    }

    // Aceita clientes conectados ao servidor.
    // Para cada cliente cria um ClientHandler e envia para o thread pool. Ignora
    // SocketException quando o servidor ja esta parando, pois isso e esperado ao
    // fechar o ServerSocket durante o shutdown.
    private void acceptClients(ServerSocket serverSocket) throws IOException {
        while (!stopped) {
            try {
                Socket clientSocket = serverSocket.accept();

                log.info("Client connected: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, processor);

                threadPool.execute(handler);
            } catch (SocketException e) {
                if (!stopped) {
                    throw e;
                }
            }
        }
    }

    // Fecha o ServerSocket do servidor.
    // Trata IOException localmente para garantir que o fluxo de shutdown nao seja
    // interrompido por falhas ao fechar o socket.
    private void closeServerSocket() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.warn("Error closing server socket: " + e.getMessage());
            }
        }
    }

    // Encerra o pool de threads do servidor.
    // Sinaliza shutdown, espera os clientes ativos terminarem ate o timeout
    // configurado e, se algum nao terminar, forca a interrupcao com shutdownNow.
    // O timeout protege contra clientes ociosos que nunca encerrariam sozinhos.
    private void shutdownThreadPool() {
        if (threadPool == null) {
            return;
        }

        if (threadPool.isShutdown()) {
            return;
        }

        threadPool.shutdown();

        try {
            if (!threadPool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
