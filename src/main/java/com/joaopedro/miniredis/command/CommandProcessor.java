package com.joaopedro.miniredis.command;

import com.joaopedro.miniredis.core.MiniRedis;
import com.joaopedro.miniredis.persistence.AppendOnlyFile;

public class CommandProcessor {
    private MiniRedis redis;
    private AppendOnlyFile aof;

    // Cria o processador de comandos.
    // Recebe uma instancia do MiniRedis e uma instancia do AOF para executar e
    // persistir comandos.
    public CommandProcessor(MiniRedis redis, AppendOnlyFile aof) {
        this.redis = redis;
        this.aof = aof;
    }

    // Processa uma linha de texto digitada pelo usuario.
    // Normaliza a entrada, identifica o comando principal e chama a funcao
    // correspondente.
    public String process(String input) {
        String response;

        if (input == null) {
            response = "ERROR empty command";
        } else {
            String[] parts = parseInput(input);

            if (parts.length == 0) {
                response = "ERROR empty command";
            } else {
                String command = parts[0].toUpperCase();

                if (command.equals("SET")) {
                    response = processSet(parts);
                } else if (command.equals("GET")) {
                    response = processGet(parts);
                } else if (command.equals("DEL")) {
                    response = processDel(parts);
                } else if (command.equals("EXISTS")) {
                    response = processExists(parts);
                } else if (command.equals("EXPIRE")) {
                    response = processExpire(parts);
                } else if (command.equals("TTL")) {
                    response = processTtl(parts);
                } else if (command.equals("PING")) {
                    response = processPing(parts);
                } else if (command.equals("KEYS")) {
                    response = processKeys(parts);
                } else if (command.equals("FLUSHALL")) {
                    response = processFlushAll(parts);
                } else if (command.equals("REWRITEAOF")) {
                    response = processRewriteAof(parts);
                } else {
                    response = "ERROR unknown command";
                }
            }
        }

        return response;
    }

    // Processa o comando SET.
    // Verifica se o usuario enviou chave e valor, salva no MiniRedis e registra o
    // comando no AOF.
    private String processSet(String[] parts) {
        String response;

        if (parts.length < 3) {
            response = "ERROR usage: SET key value";
        } else {
            response = redis.set(parts[1], parts[2]);

            aof.append("SET " + parts[1] + " " + parts[2]);
        }

        return response;
    }

    // Processa o comando GET.
    // Verifica se o usuario enviou uma chave e busca o valor correspondente no
    // MiniRedis.
    private String processGet(String[] parts) {
        String response;

        if (parts.length != 2) {
            response = "ERROR usage: GET key";
        } else {
            String value = redis.get(parts[1]);

            if (value == null) {
                response = "(nil)";
            } else {
                response = value;
            }
        }

        return response;
    }

    // Processa o comando DEL.
    // Remove a chave do MiniRedis e registra no AOF apenas se alguma chave foi
    // removida.
    private String processDel(String[] parts) {
        String response;

        if (parts.length != 2) {
            response = "ERROR usage: DEL key";
        } else {
            int result = redis.del(parts[1]);

            response = String.valueOf(result);

            if (result == 1) {
                aof.append("DEL " + parts[1]);
            }
        }

        return response;
    }

    // Processa o comando EXISTS.
    // Verifica se o usuario enviou uma chave e retorna 1 se ela existir ou 0 se nao
    // existir.
    private String processExists(String[] parts) {
        String response;

        if (parts.length != 2) {
            response = "ERROR usage: EXISTS key";
        } else {
            response = String.valueOf(redis.exists(parts[1]));
        }

        return response;
    }

    // Processa o comando EXPIRE.
    // Converte os segundos para timestamp absoluto, define a expiracao e salva
    // EXPIREAT no AOF.
    private String processExpire(String[] parts) {
        String response;

        if (parts.length != 3) {
            response = "ERROR usage: EXPIRE key seconds";
        } else {
            try {
                long seconds = Long.parseLong(parts[2]);
                long expiresAt = System.currentTimeMillis() + seconds * 1000;

                int result = redis.expireAt(parts[1], expiresAt);

                response = String.valueOf(result);

                if (result == 1) {
                    aof.append("EXPIREAT " + parts[1] + " " + expiresAt);
                }
            } catch (NumberFormatException e) {
                response = "ERROR seconds must be a number";
            }
        }

        return response;
    }

    // Processa o comando TTL.
    // Verifica se o usuario enviou uma chave e retorna o tempo restante de vida
    // dessa chave.
    private String processTtl(String[] parts) {
        String response;

        if (parts.length != 2) {
            response = "ERROR usage: TTL key";
        } else {
            response = String.valueOf(redis.ttl(parts[1]));
        }

        return response;
    }

    // Processa o comando REWRITEAOF.
    // Reescreve o arquivo AOF mantendo apenas o estado atual do banco.
    private String processRewriteAof(String[] parts) {
        String response;

        if (parts.length != 1) {
            response = "ERROR usage: REWRITEAOF";
        } else {
            aof.rewrite(redis);
            response = "OK";
        }

        return response;
    }

    // Processa o comando PING.
    // Verifica se o comando veio sem argumentos e retorna PONG para indicar que o
    // servidor esta respondendo.
    private String processPing(String[] parts) {
        String response;

        if (parts.length != 1) {
            response = "ERROR usage: PING";
        } else {
            response = "PONG";
        }

        return response;
    }

    // Processa o comando KEYS.
    // Retorna todas as chaves ativas do banco em uma unica linha separada por
    // espaco.
    private String processKeys(String[] parts) {
        String response;

        if (parts.length != 1) {
            response = "ERROR usage: KEYS";
        } else {
            String[] keys = redis.keys();

            if (keys.length == 0) {
                response = "(empty)";
            } else {
                response = joinKeys(keys);
            }
        }

        return response;
    }

    // Processa o comando FLUSHALL.
    // Remove todas as chaves do banco e registra o comando no AOF.
    private String processFlushAll(String[] parts) {
        String response;

        if (parts.length != 1) {
            response = "ERROR usage: FLUSHALL";
        } else {
            redis.flushAll();
            aof.append("FLUSHALL");

            response = "OK";
        }

        return response;
    }

    // Junta as chaves em uma unica String.
    // Percorre o array manualmente e separa as chaves por espaco.
    private String joinKeys(String[] keys) {
        String result = "";

        for (int i = 0; i < keys.length; i++) {
            if (i == 0) {
                result = keys[i];
            } else {
                result = result + " " + keys[i];
            }
        }

        return result;
    }

    // Quebra uma linha de comando em partes principais.
    // Remove espacos extras no inicio, no fim e entre comando/chave, preservando o
    // valor do SET.
    private String[] parseInput(String input) {
        String normalized = normalizeSpaces(input);

        String[] result;

        if (normalized.length() == 0) {
            result = new String[0];
        } else {
            result = normalized.split(" ", 3);
        }

        return result;
    }

    // Normaliza os espacos de uma linha de comando.
    // Remove espacos extras no inicio/fim e transforma multiplos espacos em apenas
    // um.
    private String normalizeSpaces(String input) {
        String result = "";
        boolean lastWasSpace = true;

        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);

            if (current == ' ') {
                if (!lastWasSpace) {
                    result = result + current;
                    lastWasSpace = true;
                }
            } else {
                result = result + current;
                lastWasSpace = false;
            }
        }

        if (result.length() > 0 && result.charAt(result.length() - 1) == ' ') {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }
}