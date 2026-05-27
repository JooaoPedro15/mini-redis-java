package com.joaopedro.miniredis.core;

import com.joaopedro.miniredis.core.hash.MiniHashTable;
import com.joaopedro.miniredis.core.hash.HashEntry;

public class MiniRedis {
    private MiniHashTable data;

    // Cria o banco em memoria.
    // Inicializa a tabela hash manual que sera usada para armazenar as chaves e
    // valores.
    public MiniRedis() {
        this.data = new MiniHashTable();
    }

    // Salva uma chave com um valor no banco.
    // Cria uma nova Entry sem expiracao e coloca essa Entry dentro da tabela hash.
    public String set(String key, String value) {
        data.put(key, new Entry(value));

        return "OK";
    }

    // Busca o valor de uma chave no banco.
    // Primeiro remove a chave se ela ja estiver expirada, depois busca a Entry na
    // tabela hash.
    public String get(String key) {
        String result = null;

        removeIfExpired(key);

        Entry entry = data.get(key);

        if (entry != null) {
            result = entry.getValue();
        }

        return result;
    }

    // Remove uma chave do banco.
    // Primeiro verifica se a chave expirou, depois tenta remover da tabela hash.
    // Retorna 1 se removeu e 0 se a chave nao existia.
    public int del(String key) {
        int result = 0;

        removeIfExpired(key);

        Entry removed = data.remove(key);

        if (removed != null) {
            result = 1;
        }

        return result;
    }

    // Verifica se uma chave existe no banco.
    // Primeiro remove a chave se ela estiver expirada, depois consulta a tabela
    // hash.
    public int exists(String key) {
        int result = 0;

        removeIfExpired(key);

        if (data.containsKey(key)) {
            result = 1;
        }

        return result;
    }

    // Define um tempo de expiracao para uma chave.
    // Calcula o momento futuro em milissegundos e chama expireAt para salvar esse
    // timestamp.
    public int expire(String key, long seconds) {
        int result = 0;

        long expiresAt = System.currentTimeMillis() + seconds * 1000;

        result = expireAt(key, expiresAt);

        return result;
    }

    // Retorna o tempo restante de vida de uma chave.
    // Retorna -2 se a chave nao existe, -1 se existe sem expiracao ou os segundos
    // restantes.
    public long ttl(String key) {
        long result = -2;

        removeIfExpired(key);

        Entry entry = data.get(key);

        if (entry != null) {
            if (entry.getExpiresAt() == null) {
                result = -1;
            } else {
                long millisLeft = entry.getExpiresAt() - System.currentTimeMillis();

                if (millisLeft > 0) {
                    result = millisLeft / 1000;

                    if (result == 0) {
                        result = 1;
                    }
                } else {
                    data.remove(key);
                    result = -2;
                }
            }
        }

        return result;
    }

    // Remove uma chave se ela ja tiver passado do tempo de expiracao.
    // Busca a Entry na tabela hash e, se estiver expirada, remove a chave do banco.
    private void removeIfExpired(String key) {
        Entry entry = data.get(key);

        if (entry != null && entry.isExpired()) {
            data.remove(key);
        }
    }

    // Define um tempo absoluto de expiracao para uma chave.
    // Recebe o timestamp em milissegundos e salva esse valor dentro da Entry.
    public int expireAt(String key, long expiresAt) {
        int result = 0;

        removeIfExpired(key);

        Entry entry = data.get(key);

        if (entry != null) {
            entry.setExpiresAt(expiresAt);

            result = 1;
        }

        return result;
    }

    // Retorna todas as entradas validas do banco.
    // Primeiro remove as chaves expiradas e depois devolve apenas as entradas ainda ativas.
    public HashEntry[] entries()
    {
        HashEntry[] allEntries = data.entries();
        HashEntry[] temporary = new HashEntry[allEntries.length];

        int count = 0;

        for (int i = 0; i < allEntries.length; i++)
        {
            HashEntry current = allEntries[i];

            if (current != null && current.getValue() != null)
            {
                if (current.getValue().isExpired())
                {
                    data.remove(current.getKey());
                }
                else
                {
                    temporary[count] = current;
                    count++;
                }
            }
        }

        HashEntry[] result = new HashEntry[count];

        for (int i = 0; i < count; i++)
        {
            result[i] = temporary[i];
        }

        return result;
    }

    // Remove todas as chaves do banco.
    // Chama o clear da tabela hash para apagar todos os dados em memoria.
    public void flushAll()
    {
        data.clear();
    }

    // Retorna todas as chaves ativas do banco.
    // Percorre as chaves da tabela, remove as expiradas e devolve apenas as chaves validas.
    public String[] keys()
    {
        String[] allKeys = data.keys();
        String[] temporary = new String[allKeys.length];

        int count = 0;

        for (int i = 0; i < allKeys.length; i++)
        {
            String key = allKeys[i];

            if (key != null)
            {
                removeIfExpired(key);

                if (data.containsKey(key))
                {
                    temporary[count] = key;
                    count++;
                }
            }
        }

        String[] result = new String[count];

        for (int i = 0; i < count; i++)
        {
            result[i] = temporary[i];
        }

        return result;
    }
}
