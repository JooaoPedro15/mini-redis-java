package com.joaopedro.miniredis.command;

import com.joaopedro.miniredis.core.MiniRedis;
import com.joaopedro.miniredis.persistence.AppendOnlyFile;

public class CommandProcessor
{
    private MiniRedis redis;
    private AppendOnlyFile aof;
    private Runnable shutdownHook;

    // Creates the command processor without a shutdown hook.
    // Delegates to the main constructor passing null in place of the hook, keeping
    // backwards compatibility with callers that do not need SHUTDOWN.
    public CommandProcessor(MiniRedis redis, AppendOnlyFile aof)
    {
        this(redis, aof, null);
    }

    // Creates the command processor with an optional shutdown hook.
    // Receives MiniRedis, AOF and a Runnable that will be executed when the
    // SHUTDOWN command arrives. The hook can be null to disable SHUTDOWN.
    public CommandProcessor(MiniRedis redis, AppendOnlyFile aof, Runnable shutdownHook)
    {
        this.redis = redis;
        this.aof = aof;
        this.shutdownHook = shutdownHook;
    }

    // Processes a raw line of text typed by the user.
    // Normalizes the input, identifies the leading command and dispatches to the
    // matching handler. Unknown or empty commands return an error message.
    public String process(String input)
    {
        String response;

        if (input == null)
        {
            response = "ERROR empty command";
        }
        else
        {
            String[] parts = parseInput(input);

            if (parts.length == 0)
            {
                response = "ERROR empty command";
            }
            else
            {
                String command = parts[0].toUpperCase();

                if (command.equals("SET"))
                {
                    response = processSet(parts);
                }
                else if (command.equals("GET"))
                {
                    response = processGet(parts);
                }
                else if (command.equals("DEL"))
                {
                    response = processDel(parts);
                }
                else if (command.equals("EXISTS"))
                {
                    response = processExists(parts);
                }
                else if (command.equals("EXPIRE"))
                {
                    response = processExpire(parts);
                }
                else if (command.equals("TTL"))
                {
                    response = processTtl(parts);
                }
                else if (command.equals("PING"))
                {
                    response = processPing(parts);
                }
                else if (command.equals("KEYS"))
                {
                    response = processKeys(parts);
                }
                else if (command.equals("FLUSHALL"))
                {
                    response = processFlushAll(parts);
                }
                else if (command.equals("REWRITEAOF"))
                {
                    response = processRewriteAof(parts);
                }
                else if (command.equals("SHUTDOWN"))
                {
                    response = processShutdown(parts);
                }
                else
                {
                    response = "ERROR unknown command";
                }
            }
        }

        return response;
    }

    // Handles the SET command.
    // Checks that key and value were provided, stores the value in MiniRedis and
    // appends the SET command to the AOF.
    private String processSet(String[] parts)
    {
        String response;

        if (parts.length < 3)
        {
            response = "ERROR usage: SET key value";
        }
        else
        {
            response = redis.set(parts[1], parts[2]);

            aof.append("SET " + parts[1] + " " + parts[2]);
        }

        return response;
    }

    // Handles the GET command.
    // Checks that a single key was provided and fetches the value from MiniRedis.
    // Missing keys return the conventional "(nil)" response.
    private String processGet(String[] parts)
    {
        String response;

        if (parts.length != 2)
        {
            response = "ERROR usage: GET key";
        }
        else
        {
            String value = redis.get(parts[1]);

            if (value == null)
            {
                response = "(nil)";
            }
            else
            {
                response = value;
            }
        }

        return response;
    }

    // Handles the DEL command.
    // Removes the key from MiniRedis and only appends to the AOF when an actual
    // key was deleted, avoiding noise for no-op deletes.
    private String processDel(String[] parts)
    {
        String response;

        if (parts.length != 2)
        {
            response = "ERROR usage: DEL key";
        }
        else
        {
            int result = redis.del(parts[1]);

            response = String.valueOf(result);

            if (result == 1)
            {
                aof.append("DEL " + parts[1]);
            }
        }

        return response;
    }

    // Handles the EXISTS command.
    // Checks that a single key was provided and returns 1 if the key exists or
    // 0 otherwise.
    private String processExists(String[] parts)
    {
        String response;

        if (parts.length != 2)
        {
            response = "ERROR usage: EXISTS key";
        }
        else
        {
            response = String.valueOf(redis.exists(parts[1]));
        }

        return response;
    }

    // Handles the EXPIRE command.
    // Converts the seconds argument into an absolute timestamp, sets the
    // expiration on the key and persists the change as an EXPIREAT line in the AOF.
    private String processExpire(String[] parts)
    {
        String response;

        if (parts.length != 3)
        {
            response = "ERROR usage: EXPIRE key seconds";
        }
        else
        {
            try
            {
                long seconds = Long.parseLong(parts[2]);
                long expiresAt = System.currentTimeMillis() + seconds * 1000;

                int result = redis.expireAt(parts[1], expiresAt);

                response = String.valueOf(result);

                if (result == 1)
                {
                    aof.append("EXPIREAT " + parts[1] + " " + expiresAt);
                }
            }
            catch (NumberFormatException e)
            {
                response = "ERROR seconds must be a number";
            }
        }

        return response;
    }

    // Handles the TTL command.
    // Checks that a single key was provided and returns the remaining time-to-live
    // of that key.
    private String processTtl(String[] parts)
    {
        String response;

        if (parts.length != 2)
        {
            response = "ERROR usage: TTL key";
        }
        else
        {
            response = String.valueOf(redis.ttl(parts[1]));
        }

        return response;
    }

    // Handles the SHUTDOWN command.
    // Fires the configured shutdown hook and responds OK. When no hook is
    // configured, returns an error indicating that the command is unavailable.
    private String processShutdown(String[] parts)
    {
        String response;

        if (parts.length != 1)
        {
            response = "ERROR usage: SHUTDOWN";
        }
        else if (shutdownHook == null)
        {
            response = "ERROR SHUTDOWN not available";
        }
        else
        {
            shutdownHook.run();
            response = "OK";
        }

        return response;
    }

    // Handles the REWRITEAOF command.
    // Rewrites the AOF file keeping only the current state of the database.
    private String processRewriteAof(String[] parts)
    {
        String response;

        if (parts.length != 1)
        {
            response = "ERROR usage: REWRITEAOF";
        }
        else
        {
            aof.rewrite(redis);
            response = "OK";
        }

        return response;
    }

    // Handles the PING command.
    // Checks that no arguments were sent and returns PONG to confirm the server
    // is responsive.
    private String processPing(String[] parts)
    {
        String response;

        if (parts.length != 1)
        {
            response = "ERROR usage: PING";
        }
        else
        {
            response = "PONG";
        }

        return response;
    }

    // Handles the KEYS command.
    // Returns every active key in the database as a single line separated by
    // spaces, or "(empty)" when no key is present.
    private String processKeys(String[] parts)
    {
        String response;

        if (parts.length != 1)
        {
            response = "ERROR usage: KEYS";
        }
        else
        {
            String[] keys = redis.keys();

            if (keys.length == 0)
            {
                response = "(empty)";
            }
            else
            {
                response = joinKeys(keys);
            }
        }

        return response;
    }

    // Handles the FLUSHALL command.
    // Removes every key from the database and persists the FLUSHALL line in the AOF.
    private String processFlushAll(String[] parts)
    {
        String response;

        if (parts.length != 1)
        {
            response = "ERROR usage: FLUSHALL";
        }
        else
        {
            redis.flushAll();
            aof.append("FLUSHALL");

            response = "OK";
        }

        return response;
    }

    // Joins the keys into a single String.
    // Walks the array manually and separates the keys with a single space.
    private String joinKeys(String[] keys)
    {
        String result = "";

        for (int i = 0; i < keys.length; i++)
        {
            if (i == 0)
            {
                result = keys[i];
            }
            else
            {
                result = result + " " + keys[i];
            }
        }

        return result;
    }

    // Breaks a command line into its main parts.
    // Trims surrounding whitespace and collapses repeated spaces between command
    // and key, while preserving the SET value as a single segment.
    private String[] parseInput(String input)
    {
        String normalized = normalizeSpaces(input);

        String[] result;

        if (normalized.length() == 0)
        {
            result = new String[0];
        }
        else
        {
            result = normalized.split(" ", 3);
        }

        return result;
    }

    // Normalizes the whitespace of a command line.
    // Removes leading and trailing spaces and reduces multiple consecutive spaces
    // into a single one.
    private String normalizeSpaces(String input)
    {
        String result = "";
        boolean lastWasSpace = true;

        for (int i = 0; i < input.length(); i++)
        {
            char current = input.charAt(i);

            if (current == ' ')
            {
                if (!lastWasSpace)
                {
                    result = result + current;
                    lastWasSpace = true;
                }
            }
            else
            {
                result = result + current;
                lastWasSpace = false;
            }
        }

        if (result.length() > 0 && result.charAt(result.length() - 1) == ' ')
        {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }
}
