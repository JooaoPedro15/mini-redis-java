# Manual TCP Test

Este guia mostra como testar manualmente o servidor TCP do Mini Redis Java no Windows.

## 1. Iniciar o servidor

Compile e rode o projeto pela raiz do repositorio:

```powershell
mvn compile
java -cp target\classes com.joaopedro.miniredis.Main
```

Se o Maven nao estiver disponivel no `PATH`, use o fallback com `javac`:

```powershell
New-Item -ItemType Directory -Force -Path target\classes
javac --release 17 -encoding UTF-8 -d target\classes (Get-ChildItem -Recurse src\main\java\*.java).FullName
java -cp target\classes com.joaopedro.miniredis.Main
```

O servidor deve mostrar algo parecido com:

```text
Mini Redis server started on port 6379
Max clients: 10
```

## 2. Conectar pelo PowerShell

Abra outro PowerShell e conecte na porta `6379`:

```powershell
$client = [System.Net.Sockets.TcpClient]::new("127.0.0.1", 6379)
$stream = $client.GetStream()
$reader = [System.IO.StreamReader]::new($stream)
$writer = [System.IO.StreamWriter]::new($stream)
$writer.AutoFlush = $true
$reader.ReadLine()
$reader.ReadLine()
```

As duas chamadas de `ReadLine()` devem mostrar:

```text
Mini Redis Java connected
Type commands or QUIT to disconnect
```

Para enviar comandos, use:

```powershell
$writer.WriteLine("PING")
$reader.ReadLine()
```

## 3. Testar comandos principais

Execute os comandos abaixo no mesmo cliente PowerShell:

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

Respostas esperadas:

```text
PONG
OK
joao pedro costa
1
1
um numero maior que 0
nome
OK
OK
(empty)
```

## 4. Testar QUIT

Ainda no cliente conectado:

```powershell
$writer.WriteLine("QUIT")
$reader.ReadLine()
$client.Close()
```

A resposta esperada e:

```text
Bye
```

Depois disso, o cliente desconecta, mas o servidor deve continuar rodando no outro terminal.

## 5. Testar dois clientes ao mesmo tempo

Mantenha o servidor rodando.

No PowerShell do cliente 1:

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

Em outro PowerShell, cliente 2:

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

O cliente 2 deve receber:

```text
value-from-client-1
```

Feche os dois clientes:

```powershell
$writer1.WriteLine("QUIT")
$reader1.ReadLine()
$client1.Close()

$writer2.WriteLine("QUIT")
$reader2.ReadLine()
$client2.Close()
```

## 6. Testar persistencia reiniciando o servidor

Com o servidor rodando, conecte um cliente e salve uma chave:

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

Pare o servidor com `Ctrl+C` no terminal onde ele esta rodando.

Inicie o servidor novamente e conecte outro cliente:

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

A resposta esperada e:

```text
valor-salvo
```

Para limpar o estado depois do teste:

```powershell
$writer.WriteLine("FLUSHALL")
$reader.ReadLine()
$writer.WriteLine("REWRITEAOF")
$reader.ReadLine()
$writer.WriteLine("QUIT")
$reader.ReadLine()
$client.Close()
```

## 7. O que este teste confirma

- O servidor inicia na porta configurada.
- O servidor aceita conexoes TCP.
- Cada linha enviada pelo cliente vira um comando.
- As respostas voltam pelo mesmo socket.
- `QUIT` desconecta apenas o cliente atual.
- O servidor continua rodando apos um cliente sair.
- Mais de um cliente pode usar o mesmo servidor.
- O AOF salva o estado e permite recuperar dados apos reiniciar.
