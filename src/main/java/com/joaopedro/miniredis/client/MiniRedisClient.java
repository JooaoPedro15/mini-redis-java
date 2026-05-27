package com.joaopedro.miniredis.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class MiniRedisClient
{
    private String host;
    private int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    // Cria um cliente TCP do Mini Redis.
    // Apenas guarda host e porta. A conexao so e aberta quando connect e chamado.
    public MiniRedisClient(String host, int port)
    {
        this.host = host;
        this.port = port;
    }

    // Abre a conexao TCP com o servidor.
    // Cria o socket, prepara reader e writer e le as linhas de boas-vindas enviadas pelo servidor.
    // Retorna as linhas de boas-vindas para que o chamador possa exibi-las.
    public String[] connect() throws IOException
    {
        this.socket = new Socket(host, port);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);

        String firstLine = reader.readLine();
        String secondLine = reader.readLine();

        return new String[] { firstLine, secondLine };
    }

    // Envia um comando para o servidor e devolve a resposta.
    // Escreve a linha no socket e le exatamente uma linha de resposta, pois o protocolo do servidor responde sempre em uma unica linha.
    public String sendCommand(String command) throws IOException
    {
        writer.println(command);

        return reader.readLine();
    }

    // Verifica se a linha digitada pelo usuario corresponde ao comando QUIT.
    // Ignora espacos no inicio/fim e diferenca entre maiusculas e minusculas.
    public boolean isQuitCommand(String line)
    {
        boolean result = false;

        if (line != null && line.trim().equalsIgnoreCase("QUIT"))
        {
            result = true;
        }

        return result;
    }

    // Fecha a conexao com o servidor.
    // Usa try/catch para garantir que um erro ao fechar nao quebre a aplicacao chamadora.
    public void close()
    {
        try
        {
            if (socket != null)
            {
                socket.close();
            }
        }
        catch (IOException e)
        {
            System.out.println("Error closing client socket: " + e.getMessage());
        }
    }
}
