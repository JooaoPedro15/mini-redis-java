package com.joaopedro.miniredis;

import com.joaopedro.miniredis.command.CommandProcessor;
import com.joaopedro.miniredis.core.MiniRedis;

import java.util.Scanner;

public class Main
{
    // Inicia o Mini Redis em modo terminal.
    // Cria o banco, cria o processador de comandos e fica lendo comandos ate o usuario digitar EXIT.
    public static void main(String[] args)
    {
        MiniRedis redis = new MiniRedis();
        CommandProcessor processor = new CommandProcessor(redis);
        Scanner scanner = new Scanner(System.in);

        String line = "";

        System.out.println("Mini Redis Java");
        System.out.println("Type commands or EXIT to stop.");

        while (!line.equalsIgnoreCase("EXIT"))
        {
            System.out.print("> ");
            line = scanner.nextLine();

            if (!line.equalsIgnoreCase("EXIT"))
            {
                String response = processor.process(line);
                System.out.println(response);
            }
        }

        scanner.close();
    }
}