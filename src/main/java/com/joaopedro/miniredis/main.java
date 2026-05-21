package com.joaopedro.miniredis;

import com.joaopedro.miniredis.core.Entry;
import com.joaopedro.miniredis.core.hash.MiniHashTable;

public class Main
{
    // Testa o resize automatico da MiniHashTable.
    // Insere varias chaves para passar do load factor maximo e verifica se a capacidade aumenta.
    public static void main(String[] args)
    {
        MiniHashTable table = new MiniHashTable();

        System.out.println("Capacidade inicial: " + table.capacity());

        for (int i = 0; i < 20; i++)
        {
            table.put("key" + i, new Entry("value" + i));
        }

        System.out.println("Tamanho: " + table.size());
        System.out.println("Capacidade final: " + table.capacity());

        System.out.println(table.get("key0").getValue());
        System.out.println(table.get("key10").getValue());
        System.out.println(table.get("key19").getValue());
    }
}