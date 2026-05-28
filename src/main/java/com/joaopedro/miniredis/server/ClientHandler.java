package com.joaopedro.miniredis.server;

import com.joaopedro.miniredis.command.CommandProcessor;
import com.joaopedro.miniredis.util.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable
{
    private static final Logger log = new Logger("ClientHandler");

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
    // Chama o metodo que le comandos, registra a desconexao ao final e garante que
    // o socket sera fechado mesmo em caso de erro.
    @Override
    public void run()
    {
        String clientAddress = String.valueOf(clientSocket.getInetAddress());

        try
        {
            handleClient();
        }
        catch (IOException e)
        {
            log.warn("Client connection error (" + clientAddress + "): " + e.getMessage());
        }
        finally
        {
            closeClientSocket();
            log.info("Client disconnected: " + clientAddress);
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

        while (line != null && !isQuitCommand(line))
        {
            String response = processor.process(line);

            writer.println(response);

            line = reader.readLine();
        }

        writer.println("Bye");
    }

    // Verifica se a linha recebida e o comando QUIT.
    // Remove espacos extras no inicio e no fim antes de comparar, para permitir encerrar a conexao mesmo com espacos.
    private boolean isQuitCommand(String line)
    {
        boolean result = false;

        if (line != null && line.trim().equalsIgnoreCase("QUIT"))
        {
            result = true;
        }

        return result;
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
            log.warn("Error closing client socket: " + e.getMessage());
        }
    }
}
