package com.joaopedro.miniredis;

import com.joaopedro.miniredis.server.RedisServer;

public class Main
{
    // Inicia o servidor TCP do Mini Redis.
    // Cria um RedisServer na porta 6379, registra um JVM shutdown hook para
    // encerrar de forma controlada em caso de Ctrl+C, e chama start para aceitar
    // conexoes.
    public static void main(String[] args)
    {
        RedisServer server = new RedisServer(6379);

        Thread jvmShutdownHook = new Thread(server::stop, "mini-redis-jvm-shutdown");

        Runtime.getRuntime().addShutdownHook(jvmShutdownHook);

        server.start();
    }
}
