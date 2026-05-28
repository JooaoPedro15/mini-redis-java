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

public class AppendOnlyFile
{
    private static final Logger log = new Logger("AppendOnlyFile");

    private String filePath;

    // Creates the AOF file manager.
    // Stores the file path and makes sure the parent directory exists so the
    // file can be created on the first write.
    public AppendOnlyFile(String filePath)
    {
        this.filePath = filePath;

        createParentDirectory();
    }

    // Appends a command to the end of the AOF file.
    // Opens the file in append mode, writes the command followed by a line break
    // and closes the writer. Synchronized to protect against concurrent writers.
    public synchronized void append(String command)
    {
        try
        {
            FileWriter writer = new FileWriter(filePath, true);

            writer.write(command);
            writer.write(System.lineSeparator());

            writer.close();
        }
        catch (IOException e)
        {
            log.error("Error writing AOF: " + e.getMessage());
        }
    }

    // Loads the commands persisted in the AOF file.
    // Reads the file line by line and replays each known command directly against
    // MiniRedis to rebuild the in-memory state.
    public void load(MiniRedis redis)
    {
        File file = new File(filePath);

        if (file.exists())
        {
            try
            {
                BufferedReader reader = new BufferedReader(new FileReader(file));

                String line = reader.readLine();

                while (line != null)
                {
                    replayCommand(line, redis);

                    line = reader.readLine();
                }

                reader.close();
            }
            catch (IOException e)
            {
                log.error("Error reading AOF: " + e.getMessage());
            }
        }
    }

    // Replays a single command saved in the AOF.
    // Only interprets the commands that mutate state: SET, DEL, EXPIREAT and FLUSHALL.
    private void replayCommand(String line, MiniRedis redis)
    {
        if (line != null && line.length() > 0)
        {
            String[] parts = line.split(" ", 3);
            String command = parts[0].toUpperCase();

            if (command.equals("SET"))
            {
                replaySet(parts, redis);
            }
            else if (command.equals("DEL"))
            {
                replayDel(parts, redis);
            }
            else if (command.equals("EXPIREAT"))
            {
                replayExpireAt(parts, redis);
            }
            else if (command.equals("FLUSHALL"))
            {
                replayFlushAll(parts, redis);
            }
        }
    }

    // Replays a SET command saved in the AOF.
    // Uses split with limit 3 to allow values that contain spaces.
    private void replaySet(String[] parts, MiniRedis redis)
    {
        if (parts.length == 3)
        {
            redis.set(parts[1], parts[2]);
        }
    }

    // Replays a DEL command saved in the AOF.
    // Removes the key if it exists in the rebuilt database.
    private void replayDel(String[] parts, MiniRedis redis)
    {
        if (parts.length == 2)
        {
            redis.del(parts[1]);
        }
    }

    // Replays an EXPIREAT command saved in the AOF.
    // Parses the timestamp and sets the absolute expiration on the key.
    private void replayExpireAt(String[] parts, MiniRedis redis)
    {
        if (parts.length == 3)
        {
            try
            {
                long expiresAt = Long.parseLong(parts[2]);

                redis.expireAt(parts[1], expiresAt);
            }
            catch (NumberFormatException e)
            {
                log.warn("Invalid EXPIREAT in AOF: " + parts[2]);
            }
        }
    }

    // Creates the parent directory of the AOF file when it does not exist yet.
    // Allows the file to live inside a folder such as data/appendonly.aof without
    // requiring the user to create the folder manually.
    private void createParentDirectory()
    {
        File file = new File(filePath);
        File parent = file.getParentFile();

        if (parent != null && !parent.exists())
        {
            parent.mkdirs();
        }
    }

    // Rewrites the AOF file with the current state of the database.
    // Writes every active key into a temporary file, then atomically replaces the
    // original AOF using Files.move so a crash mid-write cannot corrupt the AOF.
    public synchronized void rewrite(MiniRedis redis)
    {
        File originalFile = new File(filePath);
        File temporaryFile = new File(filePath + ".tmp");

        try
        {
            FileWriter writer = new FileWriter(temporaryFile, false);

            HashEntry[] entries = redis.entries();

            for (int i = 0; i < entries.length; i++)
            {
                HashEntry current = entries[i];

                if (current != null)
                {
                    writeEntry(writer, current);
                }
            }

            writer.close();

            Files.move(
                    temporaryFile.toPath(),
                    originalFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            log.error("Error rewriting AOF: " + e.getMessage());
        }
    }

    // Writes a single current entry into the rewritten AOF file.
    // First emits the SET line and, when the key has an expiration timestamp,
    // emits an additional EXPIREAT line so the state survives the rewrite.
    private void writeEntry(FileWriter writer, HashEntry hashEntry) throws IOException
    {
        String key = hashEntry.getKey();
        Entry entry = hashEntry.getValue();

        writer.write("SET " + key + " " + entry.getValue());
        writer.write(System.lineSeparator());

        if (entry.getExpiresAt() != null)
        {
            writer.write("EXPIREAT " + key + " " + entry.getExpiresAt());
            writer.write(System.lineSeparator());
        }
    }

    // Replays a FLUSHALL command saved in the AOF.
    // Wipes every key while reconstructing the in-memory data.
    private void replayFlushAll(String[] parts, MiniRedis redis)
    {
        if (parts.length == 1)
        {
            redis.flushAll();
        }
    }
}
