package com.joaopedro.miniredis.core.hash;

import com.joaopedro.miniredis.core.Entry;

public class HashNode
{
    private String key;
    private Entry value;
    private HashNode next;

    // Cria um no da lista ligada de um bucket.
    // Guarda a chave, a Entry associada e inicia sem proximo no.
    public HashNode(String key, Entry value)
    {
        this.key = key;
        this.value = value;
        this.next = null;
    }

    // Retorna a chave armazenada neste no.
    // Essa chave e usada para comparar entradas durante buscas e remocoes.
    public String getKey()
    {
        return key;
    }

    // Retorna a Entry armazenada neste no.
    // A Entry contem o valor real e os metadados de expiracao.
    public Entry getValue()
    {
        return value;
    }

    // Atualiza a Entry armazenada neste no.
    // Esse metodo e usado quando um put recebe uma chave que ja existe.
    public void setValue(Entry value)
    {
        this.value = value;
    }

    // Retorna o proximo no da lista ligada.
    // Permite percorrer colisoes dentro do mesmo bucket.
    public HashNode getNext()
    {
        return next;
    }

    // Define o proximo no da lista ligada.
    // Esse metodo conecta ou desconecta nos durante insercao, remocao e rehashing.
    public void setNext(HashNode next)
    {
        this.next = next;
    }
}
