package com.joaopedro.miniredis.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConfigTest
{
    @Test
    void parseWithoutArgsReturnsDefaults()
    {
        ServerConfig config = ServerConfig.parse(new String[] {});

        assertEquals(ServerConfig.DEFAULT_PORT, config.getPort());
        assertEquals(ServerConfig.DEFAULT_AOF_PATH, config.getAofPath());
        assertEquals(ServerConfig.DEFAULT_MAX_CLIENTS, config.getMaxClients());
    }

    @Test
    void defaultsFactoryReturnsSameAsParseWithoutArgs()
    {
        ServerConfig fromFactory = ServerConfig.defaults();
        ServerConfig fromParse = ServerConfig.parse(new String[] {});

        assertEquals(fromFactory.getPort(), fromParse.getPort());
        assertEquals(fromFactory.getAofPath(), fromParse.getAofPath());
        assertEquals(fromFactory.getMaxClients(), fromParse.getMaxClients());
    }

    @Test
    void parseAcceptsAllThreeFlags()
    {
        ServerConfig config = ServerConfig.parse(new String[] {
                "--port", "6380",
                "--aof", "data/test.aof",
                "--max-clients", "20"
        });

        assertEquals(6380, config.getPort());
        assertEquals("data/test.aof", config.getAofPath());
        assertEquals(20, config.getMaxClients());
    }

    @Test
    void parseAcceptsSubsetOfFlags()
    {
        ServerConfig config = ServerConfig.parse(new String[] { "--port", "9999" });

        assertEquals(9999, config.getPort());
        assertEquals(ServerConfig.DEFAULT_AOF_PATH, config.getAofPath());
        assertEquals(ServerConfig.DEFAULT_MAX_CLIENTS, config.getMaxClients());
    }

    @Test
    void parseFlagsCanAppearInAnyOrder()
    {
        ServerConfig config = ServerConfig.parse(new String[] {
                "--max-clients", "5",
                "--aof", "x.aof",
                "--port", "7000"
        });

        assertEquals(7000, config.getPort());
        assertEquals("x.aof", config.getAofPath());
        assertEquals(5, config.getMaxClients());
    }

    @Test
    void parseRejectsUnknownArgument()
    {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.parse(new String[] { "--nope", "value" }));

        assertTrue(error.getMessage().contains("--nope"), "expected message to mention --nope, got: " + error.getMessage());
    }

    @Test
    void parseRejectsMissingValueForPort()
    {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.parse(new String[] { "--port" }));

        assertTrue(error.getMessage().contains("--port"), "expected message to mention --port, got: " + error.getMessage());
    }

    @Test
    void parseRejectsMissingValueForAof()
    {
        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.parse(new String[] { "--aof" }));
    }

    @Test
    void parseRejectsMissingValueForMaxClients()
    {
        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.parse(new String[] { "--max-clients" }));
    }

    @Test
    void parseRejectsNonNumericPort()
    {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.parse(new String[] { "--port", "abc" }));

        assertTrue(error.getMessage().contains("abc"));
    }

    @Test
    void parseRejectsNonNumericMaxClients()
    {
        assertThrows(IllegalArgumentException.class,
                () -> ServerConfig.parse(new String[] { "--max-clients", "ten" }));
    }

    @Test
    void constructorRejectsPortBelowMin()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new ServerConfig(0, "data/x.aof", 10));
    }

    @Test
    void constructorRejectsPortAboveMax()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new ServerConfig(70000, "data/x.aof", 10));
    }

    @Test
    void constructorRejectsNegativePort()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new ServerConfig(-1, "data/x.aof", 10));
    }

    @Test
    void constructorRejectsEmptyAofPath()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new ServerConfig(6379, "", 10));
    }

    @Test
    void constructorRejectsBlankAofPath()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new ServerConfig(6379, "   ", 10));
    }

    @Test
    void constructorRejectsNullAofPath()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new ServerConfig(6379, null, 10));
    }

    @Test
    void constructorRejectsZeroMaxClients()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new ServerConfig(6379, "data/x.aof", 0));
    }

    @Test
    void constructorRejectsNegativeMaxClients()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new ServerConfig(6379, "data/x.aof", -5));
    }

    @Test
    void usageMentionsAllThreeFlags()
    {
        String usage = ServerConfig.usage();

        assertTrue(usage.contains("--port"), "usage should mention --port");
        assertTrue(usage.contains("--aof"), "usage should mention --aof");
        assertTrue(usage.contains("--max-clients"), "usage should mention --max-clients");
    }
}
