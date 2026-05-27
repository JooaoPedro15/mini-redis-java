package com.joaopedro.miniredis;

import com.joaopedro.miniredis.server.RedisServer;

public class Main
{
    // Inicia o servidor TCP do Mini Redis.
    // Cria um RedisServer na porta 6379 e chama start para aceitar conexoes.
    public static void main(String[] args)
    {
        RedisServer server = new RedisServer(6379);

        server.start();
    }
}