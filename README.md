# mini-redis-java

An educational Mini Redis written in Java — manual hash table, TTL, TCP server, multiple clients and AOF persistence.

This project is not a full Redis clone. The goal is to study, with hand-written code, how some pieces of an in-memory key/value store work on the inside.

## Project goal

The main goal is to practice data structures, TCP networking, concurrency, caching and persistence.

In practice the project ties together several topics:

- manual hash table implementation;
- collision handling with linked lists;
- Redis-inspired commands;
- key expiration via TTL;
- TCP server serving multiple clients;
- append-only file persistence.

## Features

Supported commands:

```text
PING
SET
GET
DEL
EXISTS
EXPIRE
TTL
KEYS
FLUSHALL
REWRITEAOF
SHUTDOWN
QUIT
```

Implemented capabilities:

- TCP server on port `6379` (configurable);
- multiple clients served by a thread pool;
- graceful shutdown via the `SHUTDOWN` command or `Ctrl+C`;
- AOF persistence at `data/appendonly.aof`;
- AOF compaction with `REWRITEAOF`;
- manual hash table;
- collisions resolved with linked lists (separate chaining);
- resize and rehashing as the table grows;
- TTL with lazy expiration;
- unit tests with JUnit Jupiter.

## Project architecture

Main layout:

```text
src/main/java/com/joaopedro/miniredis
  Main.java
  ClientMain.java
  BenchmarkMain.java
  core/
    Entry.java
    MiniRedis.java
    hash/
      HashEntry.java
      HashNode.java
      MiniHashTable.java
  command/
    CommandProcessor.java
  server/
    RedisServer.java
    ClientHandler.java
  client/
    MiniRedisClient.java
  config/
    ServerConfig.java
  persistence/
    AppendOnlyFile.java
  util/
    Logger.java
```

### `core`

Holds the main in-memory database logic.

`MiniRedis` exposes operations such as `set`, `get`, `del`, `exists`, `expire`, `ttl`, `keys` and `flushAll`. It uses `MiniHashTable` as its storage structure.

`Entry` represents a stored value. Beyond the string, it also carries metadata such as `expiresAt`, used for TTL.

### `core.hash`

Holds the manual hash table.

`MiniHashTable` manages the buckets, computes indices, handles collisions and performs resize and rehashing.

`HashNode` represents a node inside a bucket. Each node stores the key, the `Entry` and the reference to the next node in the linked list.

`HashEntry` is used to export key/value pairs in operations such as persistence and listing.

### `command`

Holds the command processor.

`CommandProcessor` receives a text line, identifies the command and calls the matching method on `MiniRedis`. It also appends state-mutating commands to the AOF.

### `server`

Holds the TCP server.

`RedisServer` opens the configured port, loads the AOF on startup and accepts incoming clients.

`ClientHandler` serves each client on a separate thread. It reads commands line by line, writes the responses back and closes the connection when `QUIT` is received.

### `client`

Holds the TCP client of Mini Redis.

`MiniRedisClient` opens a connection to the server, sends commands and returns the response. Each command receives exactly one response line, matching the server protocol.

`ClientMain` is the entry point of the interactive client. It reads commands from the terminal, forwards them to the server through `MiniRedisClient` and prints each response. It exits when `QUIT` is typed.

### `config`

Holds the server configuration.

`ServerConfig` is an immutable object with `port`, `aofPath` and `maxClients`. It validates each field in the constructor and offers `parse(String[] args)`, which understands the flags `--port`, `--aof` and `--max-clients`. Invalid arguments throw `IllegalArgumentException` with a descriptive message, and `usage()` returns the help text printed by `Main` when parsing fails.

### `persistence`

Holds the AOF persistence layer.

`AppendOnlyFile` writes commands to a file, reloads state on startup and rewrites the file with `REWRITEAOF`.

### `util`

Holds shared utilities.

`Logger` is a simple log class with three levels: `INFO`, `WARN` and `ERROR`. Each instance is bound to a component name. INFO goes to `stdout`; WARN and ERROR go to `stderr`. Every line follows the format:

```text
[yyyy-MM-dd HH:mm:ss] [LEVEL] [Component] message
```

Sample server output during a short session:

```text
[2026-05-27 21:06:10] [INFO ] [RedisServer] Server started on port 6379
[2026-05-27 21:06:10] [INFO ] [RedisServer] Max clients: 10
[2026-05-27 21:06:16] [INFO ] [RedisServer] Client connected: /127.0.0.1
[2026-05-27 21:06:16] [INFO ] [RedisServer] Shutting down Mini Redis server...
[2026-05-27 21:06:16] [INFO ] [ClientHandler] Client disconnected: /127.0.0.1
[2026-05-27 21:06:16] [INFO ] [RedisServer] Server fully stopped
```

The interactive client (`ClientMain`, `MiniRedisClient`) does not use `Logger`: its messages are part of the CLI user experience, not service logs.

## How the MiniHashTable works

`MiniHashTable` uses an array of buckets:

```text
buckets[0]
buckets[1]
buckets[2]
...
```

Each key is run through a hash function. The result chooses which array slot the key belongs to.

Simplified flow:

```text
key -> hash(key) -> index -> bucket
```

Each bucket points to a `HashNode`. When two keys land on the same bucket, a collision happens. The project resolves it with a linked list:

```text
buckets[3] -> HashNode("name") -> HashNode("city") -> null
```

This approach is known as separate chaining.

When the table grows large enough, it resizes:

1. allocate a new array with a larger capacity;
2. walk the old buckets;
3. recompute the index of every key;
4. reinsert the nodes into the new table.

That process is the rehashing step.

## How TTL works

Every `Entry` can carry an `expiresAt` field.

When a key has no expiration, `expiresAt` is `null`.

When the `EXPIRE key seconds` command runs, the system computes a future timestamp in milliseconds:

```text
expiresAt = now + seconds
```

The `TTL key` command returns:

- `-2` when the key does not exist;
- `-1` when the key exists without expiration;
- the remaining time, in seconds, when the key has expiration.

The project uses lazy expiration. That means an expired key is removed only when someone tries to read, check or list it. There is no background thread cleaning up expirations.

## How AOF works

AOF stands for append-only file.

Instead of dumping the whole database on every change, the project records the commands that mutate state:

```text
SET name Joao
EXPIREAT name 1760000000000
DEL name
FLUSHALL
```

On startup the server reads `data/appendonly.aof` and replays the commands to rebuild the in-memory state.

The `REWRITEAOF` command compacts the file. It drops unnecessary history and writes only the current state of the active keys.

Example: if a key was updated several times, the rewrite saves only the final value.

## Thread pool

`RedisServer` accepts connections in a loop and dispatches each one to a fixed-size thread pool (`Executors.newFixedThreadPool`). The pool size defaults to `10` and can be tuned with `--max-clients`. Each `ClientHandler` runs on a pool thread and reads commands sequentially from its socket; the shared `CommandProcessor` and `MiniHashTable` use `synchronized` methods to keep state consistent under concurrent access.

When the server stops, `shutdownThreadPool` calls `shutdown()`, waits up to 5 seconds for active handlers to finish and falls back to `shutdownNow()` if any of them are still running.

## How to run

Requirements:

- Java 17 or newer;
- Maven, for the main build and test flow.

Compile the project:

```powershell
mvn compile
```

Run the tests:

```powershell
mvn test
```

Start the server:

```powershell
java -cp target\classes com.joaopedro.miniredis.Main
```

With no arguments, the server starts with the default values: port `6379`, AOF at `data/appendonly.aof` and up to `10` concurrent clients.

### Server arguments

The `Main` class accepts three optional flags to customize execution:

| Flag | Type | Default | Description |
| --- | --- | --- | --- |
| `--port` | integer 1..65535 | `6379` | TCP port the server listens on |
| `--aof` | path | `data/appendonly.aof` | AOF persistence file |
| `--max-clients` | integer > 0 | `10` | thread pool size for clients |

Examples:

```powershell
# every value customized
java -cp target\classes com.joaopedro.miniredis.Main --port 6380 --aof data/test.aof --max-clients 20

# only the port
java -cp target\classes com.joaopedro.miniredis.Main --port 6380

# only the thread pool size
java -cp target\classes com.joaopedro.miniredis.Main --max-clients 50
```

Connect the Java client to a custom port:

```powershell
java -cp target\classes com.joaopedro.miniredis.ClientMain 127.0.0.1 6380
```

Invalid arguments print an error message plus the usage text and the process exits with code `2`. Example:

```text
Error: --port requires an integer, got: abc

Usage: java -cp target\classes com.joaopedro.miniredis.Main [options]
Options:
  --port <number>       TCP port (1-65535, default 6379)
  --aof <path>          AOF file path (default data/appendonly.aof)
  --max-clients <n>     Max concurrent clients (>0, default 10)
```

If Maven is not on the `PATH`, compile with `javac`:

```powershell
New-Item -ItemType Directory -Force -Path target\classes
javac --release 17 -encoding UTF-8 -d target\classes (Get-ChildItem -Recurse src\main\java\*.java).FullName
java -cp target\classes com.joaopedro.miniredis.Main
```

## How to run the Java client

With the server running, open another terminal and start the client:

```powershell
java -cp target\classes com.joaopedro.miniredis.ClientMain
```

By default the client connects to `localhost:6379`. To target another host or port, pass them as arguments:

```powershell
java -cp target\classes com.joaopedro.miniredis.ClientMain 127.0.0.1 6379
```

The client prints the server welcome lines and shows a `>` prompt. Each line you type is sent as a command and the response is printed below it.

Sample session:

```text
Connected to localhost:6379
Mini Redis Java connected
Type commands or QUIT to disconnect
> PING
PONG
> SET nome joao
OK
> GET nome
joao
> QUIT
Bye
```

To end the session, type `QUIT`. The client sends the command to the server, prints the `Bye` response and closes the socket.

## How to run the benchmark

The project ships with a simple educational benchmark used to get a feel for the hash table and basic command performance:

```powershell
java -cp target\classes com.joaopedro.miniredis.BenchmarkMain
```

By default it runs 100,000 operations per scenario. Override with an argument:

```powershell
java -cp target\classes com.joaopedro.miniredis.BenchmarkMain 500000
```

The benchmark exercises the in-memory `MiniRedis` core. It does **not** go through TCP, AOF or the command parser. The scenarios are:

- pure SET against an empty database;
- pure GET against a pre-populated database;
- pure DEL against a pre-populated database;
- mixed 50/50 workload alternating SET and GET.

> **Important:** this benchmark **does not replace JMH**. It is educational, with no statistical sampling, no per-iteration isolation and no precise warmup control. The numbers depend heavily on the machine, the JVM and the system load. Use them to compare project versions to each other, not as an absolute measure.

Sample output (Java 17, 100,000 operations):

```text
========================================================
Mini Redis - Simple Benchmark
========================================================
This is NOT JMH. Numbers are rough and machine-dependent.
Use for relative comparison, not as a definitive measure.
--------------------------------------------------------
Operations per scenario: 100000
Warmup operations:       10000
========================================================

Warmup: 10000 operations...
Warmup done.

[SET] 100000 ops in 29.92 ms = 3342067 ops/sec
[GET] 100000 ops in 10.82 ms = 9245049 ops/sec
[DEL] 100000 ops in 15.35 ms = 6514106 ops/sec
[MIX 50/50 SET+GET] 100000 ops in 30.89 ms = 3236948 ops/sec

========================================================
Done. sink=16796288030000
========================================================
```

## How to test via TCP from PowerShell

With the server running, open another PowerShell:

```powershell
$client = [System.Net.Sockets.TcpClient]::new("127.0.0.1", 6379)
$stream = $client.GetStream()
$reader = [System.IO.StreamReader]::new($stream)
$writer = [System.IO.StreamWriter]::new($stream)
$writer.AutoFlush = $true
$reader.ReadLine()
$reader.ReadLine()
```

Send commands like this:

```powershell
$writer.WriteLine("PING")
$reader.ReadLine()
```

To close the connection:

```powershell
$writer.WriteLine("QUIT")
$reader.ReadLine()
$client.Close()
```

A more detailed guide lives at:

```text
docs/manual-tcp-test.md
```

## Graceful shutdown

The server can be shut down in two controlled ways:

- sending the `SHUTDOWN` command from any connected client;
- pressing `Ctrl+C` in the terminal where the server is running.

In both cases the server:

1. stops accepting new connections;
2. closes the `ServerSocket`;
3. waits up to 5 seconds for active clients to finish;
4. shuts down the thread pool, falling back to `shutdownNow` if the timeout is hit.

The AOF does not need any special close handling: every `append` and `rewrite` opens, writes and closes the file inside a synchronized block. Operations that already received `OK` are on disk before the server responds.

## Command examples

```text
PING
PONG

SET nome joao pedro costa
OK

GET nome
joao pedro costa

EXISTS nome
1

EXPIRE nome 60
1

TTL nome
60

KEYS
nome

DEL nome
1

FLUSHALL
OK

REWRITEAOF
OK

SHUTDOWN
OK

QUIT
Bye
```

## Tests

The project has tests for:

- `MiniHashTable`;
- `MiniRedis`;
- `CommandProcessor`;
- `AppendOnlyFile`;
- `ClientHandler`;
- `RedisServer`;
- `Logger`;
- `ServerConfig`.

Run them all:

```powershell
mvn test
```

## Current limitations

- Does not implement the real RESP protocol.
- Not compatible with `redis-cli`.
- No binary persistence (snapshot/RDB).
- The parser is simple and does not handle quoting yet.
- `SHUTDOWN` is not authenticated.
- Educational project, not meant for production use.

## Future improvements

- Implement a simplified version of the RESP protocol.
- Add snapshot persistence.
- Implement additional commands.

## Status

Project under development. The current baseline already covers in-memory storage, the main commands, TTL, AOF persistence, the TCP server, graceful shutdown, a custom Java client, a simple benchmark and unit tests.
