# Manual TCP Test

This guide shows how to manually test the Mini Redis Java TCP server on Windows.

## 1. Start the server

Compile and run the project from the repository root:

```powershell
mvn compile
java -cp target\classes com.joaopedro.miniredis.Main
```

If Maven is not on the `PATH`, use the `javac` fallback:

```powershell
New-Item -ItemType Directory -Force -Path target\classes
javac --release 17 -encoding UTF-8 -d target\classes (Get-ChildItem -Recurse src\main\java\*.java).FullName
java -cp target\classes com.joaopedro.miniredis.Main
```

The server should print something like:

```text
[2026-05-27 14:32:01] [INFO ] [RedisServer] Server started on port 6379
[2026-05-27 14:32:01] [INFO ] [RedisServer] AOF path: data/appendonly.aof
[2026-05-27 14:32:01] [INFO ] [RedisServer] Max clients: 10
```

## 2. Connect from PowerShell

Open another PowerShell and connect on port `6379`:

```powershell
$client = [System.Net.Sockets.TcpClient]::new("127.0.0.1", 6379)
$stream = $client.GetStream()
$reader = [System.IO.StreamReader]::new($stream)
$writer = [System.IO.StreamWriter]::new($stream)
$writer.AutoFlush = $true
$reader.ReadLine()
$reader.ReadLine()
```

The two `ReadLine()` calls should print:

```text
Mini Redis Java connected
Type commands or QUIT to disconnect
```

Send commands like this:

```powershell
$writer.WriteLine("PING")
$reader.ReadLine()
```

## 3. Test the main commands

Run the commands below from the same PowerShell client:

```powershell
$writer.WriteLine("PING")
$reader.ReadLine()

$writer.WriteLine("SET nome joao pedro costa")
$reader.ReadLine()

$writer.WriteLine("GET nome")
$reader.ReadLine()

$writer.WriteLine("EXISTS nome")
$reader.ReadLine()

$writer.WriteLine("EXPIRE nome 60")
$reader.ReadLine()

$writer.WriteLine("TTL nome")
$reader.ReadLine()

$writer.WriteLine("KEYS")
$reader.ReadLine()

$writer.WriteLine("REWRITEAOF")
$reader.ReadLine()

$writer.WriteLine("FLUSHALL")
$reader.ReadLine()

$writer.WriteLine("KEYS")
$reader.ReadLine()
```

Expected responses:

```text
PONG
OK
joao pedro costa
1
1
a number greater than 0
nome
OK
OK
(empty)
```

## 4. Test QUIT

Still on the connected client:

```powershell
$writer.WriteLine("QUIT")
$reader.ReadLine()
$client.Close()
```

The expected response is:

```text
Bye
```

After that the client disconnects, but the server stays up in the other terminal.

## 5. Test two clients at once

Keep the server running.

On the PowerShell of client 1:

```powershell
$client1 = [System.Net.Sockets.TcpClient]::new("127.0.0.1", 6379)
$stream1 = $client1.GetStream()
$reader1 = [System.IO.StreamReader]::new($stream1)
$writer1 = [System.IO.StreamWriter]::new($stream1)
$writer1.AutoFlush = $true
$reader1.ReadLine()
$reader1.ReadLine()
$writer1.WriteLine("SET shared value-from-client-1")
$reader1.ReadLine()
```

On another PowerShell, client 2:

```powershell
$client2 = [System.Net.Sockets.TcpClient]::new("127.0.0.1", 6379)
$stream2 = $client2.GetStream()
$reader2 = [System.IO.StreamReader]::new($stream2)
$writer2 = [System.IO.StreamWriter]::new($stream2)
$writer2.AutoFlush = $true
$reader2.ReadLine()
$reader2.ReadLine()
$writer2.WriteLine("GET shared")
$reader2.ReadLine()
```

Client 2 should receive:

```text
value-from-client-1
```

Close both clients:

```powershell
$writer1.WriteLine("QUIT")
$reader1.ReadLine()
$client1.Close()

$writer2.WriteLine("QUIT")
$reader2.ReadLine()
$client2.Close()
```

## 6. Test persistence by restarting the server

With the server running, connect a client and save a key:

```powershell
$client = [System.Net.Sockets.TcpClient]::new("127.0.0.1", 6379)
$stream = $client.GetStream()
$reader = [System.IO.StreamReader]::new($stream)
$writer = [System.IO.StreamWriter]::new($stream)
$writer.AutoFlush = $true
$reader.ReadLine()
$reader.ReadLine()
$writer.WriteLine("SET persistente valor-salvo")
$reader.ReadLine()
$writer.WriteLine("QUIT")
$reader.ReadLine()
$client.Close()
```

Stop the server with `Ctrl+C` in the terminal where it is running.

Start the server again and connect another client:

```powershell
$client = [System.Net.Sockets.TcpClient]::new("127.0.0.1", 6379)
$stream = $client.GetStream()
$reader = [System.IO.StreamReader]::new($stream)
$writer = [System.IO.StreamWriter]::new($stream)
$writer.AutoFlush = $true
$reader.ReadLine()
$reader.ReadLine()
$writer.WriteLine("GET persistente")
$reader.ReadLine()
```

The expected response is:

```text
valor-salvo
```

To clean state after the test:

```powershell
$writer.WriteLine("FLUSHALL")
$reader.ReadLine()
$writer.WriteLine("REWRITEAOF")
$reader.ReadLine()
$writer.WriteLine("QUIT")
$reader.ReadLine()
$client.Close()
```

## 7. Test SHUTDOWN

With the server running, connect a client and send the `SHUTDOWN` command:

```powershell
$client = [System.Net.Sockets.TcpClient]::new("127.0.0.1", 6379)
$stream = $client.GetStream()
$reader = [System.IO.StreamReader]::new($stream)
$writer = [System.IO.StreamWriter]::new($stream)
$writer.AutoFlush = $true
$reader.ReadLine()
$reader.ReadLine()
$writer.WriteLine("SHUTDOWN")
$reader.ReadLine()
$client.Close()
```

The expected response is:

```text
OK
```

On the server terminal, you should see:

```text
Shutting down Mini Redis server...
```

And the server process exits with code 0.

To confirm that the server stopped accepting new connections, try to reconnect:

```powershell
try { [System.Net.Sockets.TcpClient]::new("127.0.0.1", 6379) } catch { "Server stopped: $($_.Exception.InnerException.Message)" }
```

The output should mention that the connection was refused, confirming that the port is no longer open.

## 8. Test shutdown via Ctrl+C

Start the server and, in the same terminal, press `Ctrl+C`. The JVM shutdown hook kicks in and the server should print:

```text
Shutting down Mini Redis server...
```

before exiting. The hook calls the same `stop` method used by the `SHUTDOWN` command, so the shutdown flow is identical.

## 9. What this test confirms

- The server starts on the configured port.
- The server accepts TCP connections.
- Each line sent by the client becomes a command.
- Responses travel back over the same socket.
- `QUIT` disconnects only the current client.
- The server keeps running after a client leaves.
- More than one client can use the same server at the same time.
- The AOF saves state and lets data survive a restart.
- `SHUTDOWN` shuts the server down gracefully and releases the port.
- `Ctrl+C` triggers the same shutdown flow via the JVM shutdown hook.
