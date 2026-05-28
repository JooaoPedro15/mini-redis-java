package com.joaopedro.miniredis.persistence;

import com.joaopedro.miniredis.core.MiniRedis;
import com.joaopedro.miniredis.core.Entry;
import com.joaopedro.miniredis.core.hash.HashEntry;
import com.joaopedro.miniredis.util.Logger;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AppendOnlyFile {
    private static final Logger log = new Logger("AppendOnlyFile");

    private String filePath;

    // Cria o gerenciador do arquivo AOF.
    // Recebe o caminho do arquivo e garante que a pasta onde ele ficara salvo
    // existe.
    public AppendOnlyFile(String filePath) {
        this.filePath = filePath;

        createParentDirectory();
    }

    // Salva um comando no final do arquivo AOF.
    // Abre o arquivo em modo append, escreve o comando e adiciona uma quebra de
    // linha.
    public synchronized void append(String command) {
        try {
            FileWriter writer = new FileWriter(filePath, true);

            writer.write(command);
            writer.write(System.lineSeparator());

            writer.close();
        } catch (IOException e) {
            log.error("Error writing AOF: " + e.getMessage());
        }
    }

    // Carrega os dados salvos no arquivo AOF.
    // Le cada linha do arquivo e reexecuta os comandos diretamente no MiniRedis.
    public void load(MiniRedis redis) {
        File file = new File(filePath);

        if (file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));

                String line = reader.readLine();

                while (line != null) {
                    replayCommand(line, redis);

                    line = reader.readLine();
                }

                reader.close();
            } catch (IOException e) {
                log.error("Error reading AOF: " + e.getMessage());
            }
        }
    }

    // Reexecuta um comando salvo no AOF.
    // Interpreta apenas comandos que alteram dados: SET, DEL e EXPIREAT.
    private void replayCommand(String line, MiniRedis redis) {
        if (line != null && line.length() > 0) {
            String[] parts = line.split(" ", 3);
            String command = parts[0].toUpperCase();

            if (command.equals("SET")) {
                replaySet(parts, redis);
            } else if (command.equals("DEL")) {
                replayDel(parts, redis);
            } else if (command.equals("EXPIREAT")) {
                replayExpireAt(parts, redis);
            } else if (command.equals("FLUSHALL")) {
                replayFlushAll(parts, redis);
            }
        }
    }

    // Reexecuta um comando SET salvo no AOF.
    // Usa split com limite 3 para permitir valores com espacos.
    private void replaySet(String[] parts, MiniRedis redis) {
        if (parts.length == 3) {
            redis.set(parts[1], parts[2]);
        }
    }

    // Reexecuta um comando DEL salvo no AOF.
    // Remove a chave caso ela exista no banco reconstruido.
    private void replayDel(String[] parts, MiniRedis redis) {
        if (parts.length == 2) {
            redis.del(parts[1]);
        }
    }

    // Reexecuta um comando EXPIREAT salvo no AOF.
    // Converte o timestamp para numero e define a expiracao absoluta da chave.
    private void replayExpireAt(String[] parts, MiniRedis redis) {
        if (parts.length == 3) {
            try {
                long expiresAt = Long.parseLong(parts[2]);

                redis.expireAt(parts[1], expiresAt);
            } catch (NumberFormatException e) {
                log.warn("Invalid EXPIREAT in AOF: " + parts[2]);
            }
        }
    }

    // Cria a pasta pai do arquivo AOF se ela ainda nao existir.
    // Isso permite salvar o arquivo dentro de uma pasta como data/appendonly.aof.
    private void createParentDirectory() {
        File file = new File(filePath);
        File parent = file.getParentFile();

        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    // Reescreve o arquivo AOF com o estado atual do banco.
    // Cria um arquivo temporario, salva apenas as chaves ativas e depois substitui
    // o AOF antigo.
    public synchronized void rewrite(MiniRedis redis) {
        File originalFile = new File(filePath);
        File temporaryFile = new File(filePath + ".tmp");

        try {
            FileWriter writer = new FileWriter(temporaryFile, false);

            HashEntry[] entries = redis.entries();

            for (int i = 0; i < entries.length; i++) {
                HashEntry current = entries[i];

                if (current != null) {
                    writeEntry(writer, current);
                }
            }

            writer.close();

            Files.move(
                    temporaryFile.toPath(),
                    originalFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Error rewriting AOF: " + e.getMessage());
        }
    }

    // Escreve uma entrada atual do banco no arquivo AOF.
    // Primeiro salva o comando SET e, se a chave tiver expiracao, salva tambem o
    // comando EXPIREAT.
    private void writeEntry(FileWriter writer, HashEntry hashEntry) throws IOException {
        String key = hashEntry.getKey();
        Entry entry = hashEntry.getValue();

        writer.write("SET " + key + " " + entry.getValue());
        writer.write(System.lineSeparator());

        if (entry.getExpiresAt() != null) {
            writer.write("EXPIREAT " + key + " " + entry.getExpiresAt());
            writer.write(System.lineSeparator());
        }
    }


    // Reexecuta um comando FLUSHALL salvo no AOF.
    // Remove todas as chaves do banco durante a reconstrucao dos dados.
    private void replayFlushAll(String[] parts, MiniRedis redis) {
        if (parts.length == 1) {
            redis.flushAll();
        }
    }
}
