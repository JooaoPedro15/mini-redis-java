package com.joaopedro.miniredis;

import com.joaopedro.miniredis.core.Entry;
import com.joaopedro.miniredis.core.hash.MiniHashTable;

public class Main
{
    public static void main(String[] args)
    {
        MiniHashTable table = new MiniHashTable();

        table.put("nome", new Entry("joao"));
        table.put("idade", new Entry("20"));
        table.put("curso", new Entry("ciencia da computacao"));

        System.out.println(table.get("nome").getValue());
        System.out.println(table.get("idade").getValue());
        System.out.println(table.get("curso").getValue());

        table.put("nome", new Entry("joao pedro"));

        System.out.println(table.get("nome").getValue());

        System.out.println(table.containsKey("nome"));
        System.out.println(table.containsKey("altura"));

        table.remove("idade");

        System.out.println(table.containsKey("idade"));
        System.out.println(table.size());
        System.out.println(table.capacity());
    }
}