package com.joaopedro.miniredis.server;

import com.joaopedro.miniredis.command.CommandProcessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable
{
    private Socket clientSocket;
    private CommandProcessor processor;

    // Cria o manipulador de um cliente TCP.
    // Guarda o socket do cliente e o processador que executa os comandos recebidos.
    public ClientHandler(Socket clientSocket, CommandProcessor processor)
    {
        this.clientSocket = clientSocket;
        this.processor = processor;
    }

    // Executa o atendimento do cliente em uma thread separada.
    // Chama o metodo que le comandos e garante que o socket sera fechado no final.
    @Override
    public void run()
    {
        try
        {
            handleClient();
        }
        catch (IOException e)
        {
            System.out.println("Client connection error: " + e.getMessage());
        }
        finally
        {
            closeClientSocket();
        }
    }

    // Le comandos enviados pelo cliente e devolve respostas.
    // Para cada linha recebida, processa o comando usando CommandProcessor e envia o resultado.
    private void handleClient() throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);

        writer.println("Mini Redis Java connected");
        writer.println("Type commands or QUIT to disconnect");

        String line = reader.readLine();

        while (line != null && !line.equalsIgnoreCase("QUIT"))
        {
            String response = processor.process(line);

            writer.println(response);

            line = reader.readLine();
        }

        writer.println("Bye");
    }

    // Fecha a conexao com o cliente.
    // Usa try/catch para evitar que um erro ao fechar o socket derrube o servidor.
    private void closeClientSocket()
    {
        try
        {
            clientSocket.close();
        }
        catch (IOException e)
        {
            System.out.println("Error closing client socket: " + e.getMessage());
        }
    }
}