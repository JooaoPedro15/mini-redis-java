package com.joaopedro.miniredis.core;

public class Entry
{
    private String value;
    private Long expiresAt;

    // Cria uma entrada de valor do Mini Redis.
    // Guarda a String recebida e inicia sem tempo de expiracao.
    public Entry(String value)
    {
        this.value = value;
        this.expiresAt = null;
    }

    // Retorna o valor armazenado na entrada.
    // Esse metodo devolve apenas a String, sem alterar metadados de expiracao.
    public String getValue()
    {
        return value;
    }

    // Atualiza o valor armazenado na entrada.
    // Substitui somente a String e preserva o tempo de expiracao atual.
    public void setValue(String value)
    {
        this.value = value;
    }

    // Retorna o timestamp de expiracao da entrada.
    // Devolve null quando a chave nao tem TTL configurado.
    public Long getExpiresAt()
    {
        return expiresAt;
    }

    // Define o timestamp de expiracao da entrada.
    // Recebe um valor em milissegundos usado para calcular se a chave expirou.
    public void setExpiresAt(Long expiresAt)
    {
        this.expiresAt = expiresAt;
    }

    // Verifica se a entrada ja expirou.
    // Compara o timestamp atual com expiresAt e retorna false quando nao ha expiracao.
    public boolean isExpired()
    {
        boolean result = false;

        if (expiresAt != null && System.currentTimeMillis() >= expiresAt)
        {
            result = true;
        }

        return result;
    }
}
