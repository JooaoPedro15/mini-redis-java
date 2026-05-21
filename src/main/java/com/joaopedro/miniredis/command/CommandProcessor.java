package com.joaopedro.miniredis.command;

import com.joaopedro.miniredis.core.MiniRedis;

public class CommandProcessor
{
    private MiniRedis redis;

    // Cria o processador de comandos.
    // Recebe uma instancia do MiniRedis para conseguir executar os comandos digitados pelo usuario.
    public CommandProcessor(MiniRedis redis)
    {
        this.redis = redis;
    }

    // Processa uma linha de texto digitada pelo usuario.
    // Separa o comando principal e chama a funcao correta para SET, GET, DEL, EXISTS, EXPIRE ou TTL.
    public String process(String input)
    {
        String response;

        if (input == null || input.length() == 0)
        {
            response = "ERROR empty command";
        }
        else
        {
            String[] parts = input.split(" ", 3);
            String command = parts[0].toUpperCase();

            if (command.equals("SET"))
            {
                response = processSet(parts);
            }
            else if (command.equals("GET"))
            {
                response = processGet(parts);
            }
            else if (command.equals("DEL"))
            {
                response = processDel(parts);
            }
            else if (command.equals("EXISTS"))
            {
                response = processExists(parts);
            }
            else if (command.equals("EXPIRE"))
            {
                response = processExpire(parts);
            }
            else if (command.equals("TTL"))
            {
                response = processTtl(parts);
            }
            else
            {
                response = "ERROR unknown command";
            }
        }

        return response;
    }

    // Processa o comando SET.
    // Verifica se o usuario enviou chave e valor, depois salva os dados usando o MiniRedis.
    private String processSet(String[] parts)
    {
        String response;

        if (parts.length < 3)
        {
            response = "ERROR usage: SET key value";
        }
        else
        {
            response = redis.set(parts[1], parts[2]);
        }

        return response;
    }

    // Processa o comando GET.
    // Verifica se o usuario enviou uma chave e busca o valor correspondente no MiniRedis.
    private String processGet(String[] parts)
    {
        String response;

        if (parts.length != 2)
        {
            response = "ERROR usage: GET key";
        }
        else
        {
            String value = redis.get(parts[1]);

            if (value == null)
            {
                response = "(nil)";
            }
            else
            {
                response = value;
            }
        }

        return response;
    }

    // Processa o comando DEL.
    // Verifica se o usuario enviou uma chave e remove essa chave do MiniRedis.
    private String processDel(String[] parts)
    {
        String response;

        if (parts.length != 2)
        {
            response = "ERROR usage: DEL key";
        }
        else
        {
            response = String.valueOf(redis.del(parts[1]));
        }

        return response;
    }

    // Processa o comando EXISTS.
    // Verifica se o usuario enviou uma chave e retorna 1 se ela existir ou 0 se nao existir.
    private String processExists(String[] parts)
    {
        String response;

        if (parts.length != 2)
        {
            response = "ERROR usage: EXISTS key";
        }
        else
        {
            response = String.valueOf(redis.exists(parts[1]));
        }

        return response;
    }

    // Processa o comando EXPIRE.
    // Verifica se o usuario enviou chave e segundos, converte os segundos para numero e define a expiracao.
    private String processExpire(String[] parts)
    {
        String response;

        if (parts.length != 3)
        {
            response = "ERROR usage: EXPIRE key seconds";
        }
        else
        {
            try
            {
                long seconds = Long.parseLong(parts[2]);
                response = String.valueOf(redis.expire(parts[1], seconds));
            }
            catch (NumberFormatException e)
            {
                response = "ERROR seconds must be a number";
            }
        }

        return response;
    }

    // Processa o comando TTL.
    // Verifica se o usuario enviou uma chave e retorna o tempo restante de vida dessa chave.
    private String processTtl(String[] parts)
    {
        String response;

        if (parts.length != 2)
        {
            response = "ERROR usage: TTL key";
        }
        else
        {
            response = String.valueOf(redis.ttl(parts[1]));
        }

        return response;
    }
}