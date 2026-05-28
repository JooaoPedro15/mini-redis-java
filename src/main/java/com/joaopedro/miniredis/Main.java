package com.joaopedro.miniredis;

import com.joaopedro.miniredis.config.ServerConfig;
import com.joaopedro.miniredis.server.RedisServer;

public class Main
{
    // Inicia o servidor TCP do Mini Redis.
    // Faz o parsing dos argumentos para construir um ServerConfig, cria o
    // RedisServer com essa configuracao, registra um JVM shutdown hook para
    // encerrar de forma controlada em caso de Ctrl+C e chama start para aceitar
    // conexoes. Em caso de argumentos invalidos, imprime mensagem + uso e
    // encerra com exit code 2.
    public static void main(String[] args)
    {
        ServerConfig config;

        try
        {
            config = ServerConfig.parse(args);
        }
        catch (IllegalArgumentException e)
        {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println(ServerConfig.usage());

            System.exit(2);

            return;
        }

        RedisServer server = new RedisServer(config);

        Thread jvmShutdownHook = new Thread(server::stop, "mini-redis-jvm-shutdown");

        Runtime.getRuntime().addShutdownHook(jvmShutdownHook);

        server.start();
    }
}
