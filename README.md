# mini-redis-java

Um Mini Redis educacional feito em Java, com tabela hash manual, TTL, servidor TCP, multiplos clientes e persistencia AOF.

Este projeto nao tenta ser uma copia completa do Redis. A ideia e estudar, com codigo proprio, como algumas pecas de um banco chave-valor em memoria funcionam por dentro.

## Objetivo do projeto

O objetivo principal e praticar conceitos de estruturas de dados, servidor TCP, concorrencia, cache e persistencia.

Na pratica, o projeto junta alguns temas importantes:

- implementacao manual de tabela hash;
- tratamento de colisao com lista ligada;
- comandos inspirados no Redis;
- expiracao de chaves com TTL;
- servidor TCP com multiplos clientes;
- persistencia em arquivo append-only.

## Funcionalidades

Comandos suportados:

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

Recursos implementados:

- servidor TCP na porta `6379`;
- multiplos clientes usando thread pool;
- shutdown gracioso via comando `SHUTDOWN` ou `Ctrl+C`;
- persistencia AOF em `data/appendonly.aof`;
- compactacao do AOF com `REWRITEAOF`;
- tabela hash manual;
- colisao por lista ligada;
- resize e rehashing quando a tabela cresce;
- TTL com expiracao preguicosa;
- testes unitarios com JUnit Jupiter.

## Arquitetura do projeto

Estrutura principal:

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

Contem a logica principal do banco em memoria.

`MiniRedis` oferece operacoes como `set`, `get`, `del`, `exists`, `expire`, `ttl`, `keys` e `flushAll`. Ele usa a `MiniHashTable` como estrutura de armazenamento.

`Entry` representa o valor salvo. Alem da string, ela guarda metadados como `expiresAt`, usado para TTL.

### `core.hash`

Contem a tabela hash manual.

`MiniHashTable` gerencia os buckets, calcula indices, trata colisao, faz resize e rehashing.

`HashNode` representa um no dentro de um bucket. Cada no guarda a chave, a `Entry` e a referencia para o proximo no da lista ligada.

`HashEntry` e usado para exportar pares chave/valor em operacoes como persistencia e listagem.

### `command`

Contem o processador de comandos.

`CommandProcessor` recebe uma linha de texto, identifica o comando e chama o metodo correspondente em `MiniRedis`. Ele tambem registra comandos que alteram estado no AOF.

### `server`

Contem o servidor TCP.

`RedisServer` abre a porta configurada, carrega o AOF ao iniciar e aceita conexoes de clientes.

`ClientHandler` atende cada cliente em uma thread separada. Ele le comandos por linha, envia respostas e encerra a conexao quando recebe `QUIT`.

### `client`

Contem o cliente TCP do Mini Redis.

`MiniRedisClient` abre a conexao com o servidor, envia comandos e devolve a resposta. Cada comando recebe exatamente uma linha de resposta, seguindo o protocolo do servidor.

`ClientMain` e o ponto de entrada do cliente interativo. Ele le comandos do terminal, envia para o servidor pelo `MiniRedisClient` e imprime cada resposta. Encerra ao digitar `QUIT`.

### `persistence`

Contem a persistencia AOF.

`AppendOnlyFile` grava comandos em arquivo, recarrega o estado ao iniciar e reescreve o arquivo com `REWRITEAOF`.

### `config`

Contem a configuracao do servidor.

`ServerConfig` e um objeto imutavel com `port`, `aofPath` e `maxClients`. Ele valida cada campo no construtor e oferece um parser `parse(String[] args)` que entende as flags `--port`, `--aof` e `--max-clients`. Argumentos invalidos viram `IllegalArgumentException` com mensagem explicativa, e `usage()` devolve o texto de ajuda usado pelo `Main` quando o parsing falha.

### `util`

Contem utilitarios compartilhados.

`Logger` e uma classe simples de log com tres niveis: `INFO`, `WARN` e `ERROR`. Cada instancia recebe o nome do componente que esta logando. INFO vai para `stdout`; WARN e ERROR vao para `stderr`. Todas as linhas seguem o formato:

```text
[yyyy-MM-dd HH:mm:ss] [LEVEL] [Component] mensagem
```

Exemplos reais do servidor em uma sessao curta:

```text
[2026-05-27 21:06:10] [INFO ] [RedisServer] Server started on port 6379
[2026-05-27 21:06:10] [INFO ] [RedisServer] Max clients: 10
[2026-05-27 21:06:16] [INFO ] [RedisServer] Client connected: /127.0.0.1
[2026-05-27 21:06:16] [INFO ] [RedisServer] Shutting down Mini Redis server...
[2026-05-27 21:06:16] [INFO ] [ClientHandler] Client disconnected: /127.0.0.1
[2026-05-27 21:06:16] [INFO ] [RedisServer] Server fully stopped
```

O cliente CLI (`ClientMain`, `MiniRedisClient`) nao usa o `Logger`: as mensagens do cliente sao parte da UX interativa, nao logs de servico.

## Como funciona a MiniHashTable

A `MiniHashTable` usa um array de buckets:

```text
buckets[0]
buckets[1]
buckets[2]
...
```

Cada chave passa por uma funcao de hash. O resultado define em qual posicao do array a chave deve ficar.

Fluxo simplificado:

```text
key -> hash(key) -> index -> bucket
```

Cada bucket aponta para um `HashNode`. Quando duas chaves caem no mesmo bucket, acontece uma colisao. O projeto resolve isso com lista ligada:

```text
buckets[3] -> HashNode("name") -> HashNode("city") -> null
```

Esse modelo e conhecido como separate chaining.

Quando a tabela cresce demais, ela faz resize:

1. cria um novo array com capacidade maior;
2. percorre os buckets antigos;
3. recalcula o indice de cada chave;
4. reinsere os nos na nova tabela.

Esse processo e o rehashing.

## Como funciona o TTL

Cada `Entry` pode ter um campo `expiresAt`.

Quando uma chave nao tem expiracao, `expiresAt` fica `null`.

Quando o comando `EXPIRE key seconds` e usado, o sistema calcula um timestamp futuro em milissegundos:

```text
expiresAt = agora + segundos
```

O comando `TTL key` retorna:

- `-2` se a chave nao existe;
- `-1` se a chave existe sem expiracao;
- o tempo restante, em segundos, se a chave tem expiracao.

O projeto usa lazy expiration. Isso significa que a chave expirada e removida quando alguem tenta acessar, consultar ou listar essa chave. Nao existe uma thread separada limpando expiracoes em segundo plano.

## Como funciona o AOF

AOF significa append-only file.

Em vez de salvar o banco inteiro a cada mudanca, o projeto grava os comandos que alteram estado:

```text
SET name Joao
EXPIREAT name 1760000000000
DEL name
FLUSHALL
```

Ao iniciar, o servidor le `data/appendonly.aof` e reexecuta os comandos para reconstruir o estado em memoria.

O comando `REWRITEAOF` compacta o arquivo. Ele remove historico desnecessario e grava apenas o estado atual das chaves ativas.

Exemplo: se uma chave foi alterada varias vezes, o rewrite salva apenas o valor final.

## Como rodar

Requisitos:

- Java 17 ou superior;
- Maven, para o fluxo principal de build e testes.

Compile o projeto:

```powershell
mvn compile
```

Rode os testes:

```powershell
mvn test
```

Inicie o servidor:

```powershell
java -cp target\classes com.joaopedro.miniredis.Main
```

Sem argumentos, o servidor inicia com os valores padrao: porta `6379`, AOF em `data/appendonly.aof` e ate `10` clientes simultaneos.

### Argumentos do servidor

A classe `Main` aceita tres flags opcionais para customizar a execucao:

| Flag | Tipo | Default | Descricao |
| --- | --- | --- | --- |
| `--port` | inteiro 1..65535 | `6379` | porta TCP em que o servidor escuta |
| `--aof` | caminho | `data/appendonly.aof` | arquivo de persistencia AOF |
| `--max-clients` | inteiro > 0 | `10` | tamanho do thread pool de clientes |

Exemplos:

```powershell
# todos os valores customizados
java -cp target\classes com.joaopedro.miniredis.Main --port 6380 --aof data/test.aof --max-clients 20

# apenas a porta
java -cp target\classes com.joaopedro.miniredis.Main --port 6380

# apenas o thread pool
java -cp target\classes com.joaopedro.miniredis.Main --max-clients 50
```

Conectar o cliente Java em uma porta customizada:

```powershell
java -cp target\classes com.joaopedro.miniredis.ClientMain 127.0.0.1 6380
```

Argumentos invalidos imprimem mensagem de erro + uso e o processo sai com exit code `2`. Exemplo:

```text
Error: --port requires an integer, got: abc

Usage: java -cp target\classes com.joaopedro.miniredis.Main [options]
Options:
  --port <number>       TCP port (1-65535, default 6379)
  --aof <path>          AOF file path (default data/appendonly.aof)
  --max-clients <n>     Max concurrent clients (>0, default 10)
```

Se o Maven nao estiver disponivel no `PATH`, compile com `javac`:

```powershell
New-Item -ItemType Directory -Force -Path target\classes
javac --release 17 -encoding UTF-8 -d target\classes (Get-ChildItem -Recurse src\main\java\*.java).FullName
java -cp target\classes com.joaopedro.miniredis.Main
```

## Como rodar o cliente Java

Com o servidor ja rodando, abra outro terminal e inicie o cliente:

```powershell
java -cp target\classes com.joaopedro.miniredis.ClientMain
```

Por padrao, o cliente conecta em `localhost:6379`. Para apontar para outro host ou porta, passe os argumentos:

```powershell
java -cp target\classes com.joaopedro.miniredis.ClientMain 127.0.0.1 6379
```

O cliente exibe as linhas de boas-vindas do servidor e mostra o prompt `>`. Cada linha digitada e enviada como comando e a resposta e impressa logo abaixo.

Exemplo de sessao:

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

Para encerrar a sessao, digite `QUIT`. O cliente envia o comando ao servidor, imprime a resposta `Bye` e fecha o socket.

## Como rodar o benchmark

O projeto inclui um benchmark simples e educacional para ter uma nocao do desempenho da tabela hash e dos comandos basicos:

```powershell
java -cp target\classes com.joaopedro.miniredis.BenchmarkMain
```

Por padrao roda 100 mil operacoes por cenario. Para mudar, passe o numero como argumento:

```powershell
java -cp target\classes com.joaopedro.miniredis.BenchmarkMain 500000
```

O benchmark mede o nucleo do `MiniRedis` em memoria. Ele **nao** passa pelo TCP, pelo AOF nem pelo parser de comandos. Os cenarios sao:

- SET puro em banco vazio;
- GET puro em banco pre-populado;
- DEL puro em banco pre-populado;
- carga mista 50/50 alternando SET e GET.

> **Importante:** este benchmark **nao substitui o JMH**. Ele e didatico, sem amostragem estatistica, sem isolamento de medicao por iteracao e sem controle fino de warmup. Os numeros dependem fortemente da maquina, da JVM e da carga do sistema. Use para comparar versoes do projeto entre si, nao como medida absoluta.

Exemplo de saida (Java 17, 100 mil operacoes):

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

## Como testar via TCP no PowerShell

Com o servidor rodando, abra outro PowerShell:

```powershell
$client = [System.Net.Sockets.TcpClient]::new("127.0.0.1", 6379)
$stream = $client.GetStream()
$reader = [System.IO.StreamReader]::new($stream)
$writer = [System.IO.StreamWriter]::new($stream)
$writer.AutoFlush = $true
$reader.ReadLine()
$reader.ReadLine()
```

Envie comandos assim:

```powershell
$writer.WriteLine("PING")
$reader.ReadLine()
```

Para encerrar a conexao:

```powershell
$writer.WriteLine("QUIT")
$reader.ReadLine()
$client.Close()
```

Um guia mais detalhado esta em:

```text
docs/manual-tcp-test.md
```

## Exemplos de comandos

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

## Shutdown gracioso

O servidor pode ser encerrado de duas formas controladas:

- enviando o comando `SHUTDOWN` por qualquer cliente conectado;
- pressionando `Ctrl+C` no terminal onde o servidor esta rodando.

Em ambos os casos, o servidor:

1. para de aceitar novas conexoes;
2. fecha o `ServerSocket`;
3. espera ate 5 segundos os clientes ativos terminarem;
4. encerra o thread pool, com `shutdownNow` como fallback se o timeout estourar.

O AOF nao precisa ser fechado de forma especial: cada `append` e `rewrite` abre, escreve e fecha o arquivo dentro de um bloco sincronizado. Operacoes que ja receberam `OK` estao no disco antes de o servidor responder.

## Testes

O projeto tem testes para:

- `MiniHashTable`;
- `MiniRedis`;
- `CommandProcessor`;
- `AppendOnlyFile`;
- `ClientHandler`;
- `RedisServer`;
- `Logger`;
- `ServerConfig`.

Rodar todos:

```powershell
mvn test
```

## Limitacoes atuais

- Nao implementa o protocolo RESP real.
- Nao e compativel com `redis-cli`.
- Nao tem persistencia binaria.
- O parser e simples e ainda nao trata aspas.
- O servidor nao tem shutdown gracioso.
- O projeto e educacional, nao indicado para uso em producao.

## Melhorias futuras

- Implementar uma versao simplificada do protocolo RESP.
- Adicionar snapshot.
- Implementar comandos adicionais.

## Status

Projeto em desenvolvimento. A base atual ja cobre armazenamento em memoria, comandos principais, TTL, persistencia AOF, servidor TCP e testes unitarios.
