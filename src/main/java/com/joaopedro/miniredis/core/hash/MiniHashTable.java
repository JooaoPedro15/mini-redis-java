package com.joaopedro.miniredis.core.hash;

import com.joaopedro.miniredis.core.Entry;

public class MiniHashTable
{
    private static final int DEFAULT_CAPACITY = 16;
    private static final double MAX_LOAD_FACTOR = 0.75;

    private HashNode[] buckets;
    private int size;

    // Cria uma tabela hash vazia.
    // Inicializa o array de buckets com a capacidade padrao e define o tamanho como zero.
    public MiniHashTable()
    {
        this.buckets = new HashNode[DEFAULT_CAPACITY];
        this.size = 0;
    }

    // Insere ou atualiza uma chave na tabela hash.
    // Primeiro valida a chave e procura se ela ja existe.
    // Se existir, atualiza o valor. Se nao existir, verifica se precisa aumentar a tabela e cria um novo no.
    public synchronized void put(String key, Entry value)
    {
        validateKey(key);

        HashNode current = findNode(key);

        if (current != null)
        {
            current.setValue(value);
        }
        else
        {
            if (shouldResize())
            {
                resize();
            }

            int index = getIndex(key);

            HashNode newNode = new HashNode(key, value);

            newNode.setNext(buckets[index]);
            buckets[index] = newNode;

            size++;
        }
    }

    // Busca uma chave na tabela hash.
    // Usa findNode para procurar o no correto dentro do bucket calculado pela funcao hash.
    // Se encontrar a chave, retorna a Entry associada. Se nao encontrar, retorna null.
    public synchronized Entry get(String key)
    {
        validateKey(key);

        Entry result = null;

        HashNode node = findNode(key);

        if (node != null)
        {
            result = node.getValue();
        }

        return result;
    }

    // Remove uma chave da tabela hash.
    // Primeiro calcula o indice e percorre a lista usando dois ponteiros: previous e current.
    // Se encontrar a chave, remove o no da lista e retorna a Entry removida.
    public synchronized Entry remove(String key)
    {
        validateKey(key);

        Entry result = null;

        int index = getIndex(key);
        HashNode current = buckets[index];
        HashNode previous = null;

        boolean found = false;

        while (current != null && !found)
        {
            if (keysAreEqual(current.getKey(), key))
            {
                found = true;
                result = current.getValue();

                if (previous == null)
                {
                    buckets[index] = current.getNext();
                }
                else
                {
                    previous.setNext(current.getNext());
                }

                size--;
            }
            else
            {
                previous = current;
                current = current.getNext();
            }
        }

        return result;
    }

    // Verifica se uma chave existe na tabela hash.
    // Usa o metodo get para tentar encontrar a chave e retorna true se ela existir.
    public synchronized boolean containsKey(String key)
    {
        boolean result = false;

        if (get(key) != null)
        {
            result = true;
        }

        return result;
    }

    // Retorna a quantidade de chaves armazenadas.
    // O valor e atualizado sempre que uma chave nova entra ou uma chave existente e removida.
    public synchronized int size()
    {
        return size;
    }

    // Retorna a capacidade atual da tabela.
    // A capacidade e o tamanho do array de buckets.
    public synchronized int capacity()
    {
        return buckets.length;
    }

    // Procura um no pela chave dentro da tabela.
    // Primeiro calcula o indice da chave e depois percorre a lista ligada daquele bucket.
    private HashNode findNode(String key)
    {
        int index = getIndex(key);
        HashNode current = buckets[index];

        while (current != null && !keysAreEqual(current.getKey(), key))
        {
            current = current.getNext();
        }

        return current;
    }

    // Calcula o indice em que uma chave deve ficar.
    // Primeiro gera o hash da chave e depois usa o resto da divisao pela capacidade do array.
    private int getIndex(String key)
    {
        int index = hash(key) % buckets.length;

        return index;
    }

    // Gera um numero inteiro a partir de uma String.
    // Percorre cada caractere da chave e acumula um valor usando multiplicacao por 31.
    private int hash(String key)
    {
        int result = 0;

        for (int i = 0; i < key.length(); i++)
        {
            result = 31 * result + key.charAt(i);
        }

        result = result & 0x7fffffff;

        return result;
    }

    // Compara duas Strings manualmente.
    // Primeiro verifica se elas existem e tem o mesmo tamanho, depois compara caractere por caractere.
    private boolean keysAreEqual(String a, String b)
    {
        boolean result = false;

        if (a != null && b != null && a.length() == b.length())
        {
            result = true;

            for (int i = 0; i < a.length(); i++)
            {
                if (a.charAt(i) != b.charAt(i))
                {
                    result = false;
                }
            }
        }

        return result;
    }

    // Verifica se a tabela precisa aumentar de tamanho.
    // Calcula o load factor considerando a proxima insercao e compara com o limite maximo.
    private boolean shouldResize()
    {
        boolean result = false;

        double loadFactor = (double) (size + 1) / buckets.length;

        if (loadFactor > MAX_LOAD_FACTOR)
        {
            result = true;
        }

        return result;
    }

    // Aumenta a capacidade da tabela hash.
    // Cria um novo array com o dobro do tamanho e reinsere todos os nos recalculando seus indices.
    private void resize()
    {
        HashNode[] oldBuckets = buckets;

        buckets = new HashNode[oldBuckets.length * 2];
        size = 0;

        for (int i = 0; i < oldBuckets.length; i++)
        {
            HashNode current = oldBuckets[i];

            while (current != null)
            {
                HashNode next = current.getNext();

                current.setNext(null);
                insertExistingNode(current);

                current = next;
            }
        }
    }

    // Insere um no ja existente na nova tabela durante o resize.
    // Recalcula o indice da chave com base na nova capacidade e coloca o no no inicio do bucket.
    private void insertExistingNode(HashNode node)
    {
        int index = getIndex(node.getKey());

        node.setNext(buckets[index]);
        buckets[index] = node;

        size++;
    }

    // Valida se a chave pode ser usada na tabela.
    // Impede chave nula ou vazia, porque a funcao hash depende dos caracteres da chave.
    private void validateKey(String key)
    {
        if (key == null || key.length() == 0)
        {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
    }

    // Retorna todas as entradas armazenadas na tabela hash.
    // Percorre todos os buckets e copia cada chave encontrada para um array de HashEntry.
    public synchronized HashEntry[] entries()
    {
        HashEntry[] result = new HashEntry[size];

        int position = 0;

        for (int i = 0; i < buckets.length; i++)
        {
            HashNode current = buckets[i];

            while (current != null)
            {
                result[position] = new HashEntry(current.getKey(), current.getValue());
                position++;

                current = current.getNext();
            }
        }

        return result;
    }


    // Remove todas as chaves da tabela hash.
    // Cria um novo array de buckets com a capacidade padrao e zera a quantidade de elementos.
    public synchronized void clear()
    {
        this.buckets = new HashNode[DEFAULT_CAPACITY];
        this.size = 0;
    }

    // Retorna todas as chaves armazenadas na tabela hash.
    // Percorre todos os buckets e copia cada chave encontrada para um array de String.
    public synchronized String[] keys()
    {
        String[] result = new String[size];

        int position = 0;

        for (int i = 0; i < buckets.length; i++)
        {
            HashNode current = buckets[i];

            while (current != null)
            {
                result[position] = current.getKey();
                position++;

                current = current.getNext();
            }
        }

        return result;
    }
}
