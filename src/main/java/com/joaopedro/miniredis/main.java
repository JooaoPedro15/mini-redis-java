package com.joaopedro.miniredis;

import com.joaopedro.miniredis.core.MiniRedis;

public class Main
{
    // Testa os comandos principais do MiniRedis.
    // Cria o banco, salva chaves, busca valores, testa existencia, expiracao, TTL e remocao.
    public static void main(String[] args) throws InterruptedException
    {
        MiniRedis redis = new MiniRedis();

        System.out.println(redis.set("nome", "joao"));
        System.out.println(redis.get("nome"));
        System.out.println(redis.exists("nome"));
        System.out.println(redis.ttl("nome"));

        System.out.println(redis.expire("nome", 3));
        System.out.println(redis.ttl("nome"));

        Thread.sleep(4000);

        System.out.println(redis.get("nome"));
        System.out.println(redis.exists("nome"));
        System.out.println(redis.ttl("nome"));

        System.out.println(redis.set("idade", "20"));
        System.out.println(redis.get("idade"));

        System.out.println(redis.del("idade"));
        System.out.println(redis.get("idade"));
    }
}