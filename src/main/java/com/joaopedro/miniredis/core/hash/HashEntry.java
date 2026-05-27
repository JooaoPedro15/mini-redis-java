package com.joaopedro.miniredis.core.hash;

import com.joaopedro.miniredis.core.Entry;

public class HashEntry
{
    private String key;
    private Entry value;

    // Cria uma entrada exportavel da tabela hash.
    // Guarda uma chave e a Entry associada a essa chave.
    public HashEntry(String key, Entry value)
    {
        this.key = key;
        this.value = value;
    }

    // Retorna a chave armazenada nesta entrada.
    // Esse metodo e usado para percorrer os dados atuais da tabela.
    public String getKey()
    {
        return key;
    }

    // Retorna o valor armazenado nesta entrada.
    // Esse metodo permite acessar o valor e a expiracao da chave.
    public Entry getValue()
    {
        return value;
    }
}