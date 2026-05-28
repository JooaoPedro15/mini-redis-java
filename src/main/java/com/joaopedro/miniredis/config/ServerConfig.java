package com.joaopedro.miniredis.config;

public class ServerConfig {
    public static final int DEFAULT_PORT = 6379;
    public static final String DEFAULT_AOF_PATH = "data/appendonly.aof";
    public static final int DEFAULT_MAX_CLIENTS = 10;

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    private int port;
    private String aofPath;
    private int maxClients;

    // Cria uma configuracao validada do servidor.
    // Verifica cada campo no momento da construcao para que o objeto so exista
    // em estado valido. Falhas viram IllegalArgumentException com mensagem clara.
    public ServerConfig(int port, String aofPath, int maxClients) {
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException(
                    "port must be between " + MIN_PORT + " and " + MAX_PORT + ", got: " + port);
        }

        if (aofPath == null || aofPath.trim().isEmpty()) {
            throw new IllegalArgumentException("aofPath must not be empty");
        }

        if (maxClients < 1) {
            throw new IllegalArgumentException("maxClients must be greater than 0, got: " + maxClients);
        }

        this.port = port;
        this.aofPath = aofPath;
        this.maxClients = maxClients;
    }

    // Retorna a porta TCP em que o servidor deve escutar.
    public int getPort() {
        return port;
    }

    // Retorna o caminho do arquivo AOF usado para persistencia.
    public String getAofPath() {
        return aofPath;
    }

    // Retorna o numero maximo de clientes simultaneos atendidos pelo thread pool.
    public int getMaxClients() {
        return maxClients;
    }

    // Constroi um ServerConfig com todos os valores padrao.
    // Usado como atalho em testes e em chamadores que nao precisam customizar nada.
    public static ServerConfig defaults() {
        return new ServerConfig(DEFAULT_PORT, DEFAULT_AOF_PATH, DEFAULT_MAX_CLIENTS);
    }

    // Faz o parsing dos argumentos da linha de comando.
    // Aceita as flags --port, --aof e --max-clients, cada uma seguida do valor.
    // Qualquer argumento desconhecido, valor ausente ou valor invalido dispara
    // IllegalArgumentException. Argumentos nao informados ficam com o default.
    public static ServerConfig parse(String[] args) {
        int port = DEFAULT_PORT;
        String aofPath = DEFAULT_AOF_PATH;
        int maxClients = DEFAULT_MAX_CLIENTS;

        int i = 0;

        while (i < args.length) {
            String arg = args[i];

            if (arg.equals("--port")) {
                port = readIntValue(args, i, "--port");
                i = i + 2;
            } else if (arg.equals("--aof")) {
                aofPath = readStringValue(args, i, "--aof");
                i = i + 2;
            } else if (arg.equals("--max-clients")) {
                maxClients = readIntValue(args, i, "--max-clients");
                i = i + 2;
            } else {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        return new ServerConfig(port, aofPath, maxClients);
    }

    // Le o proximo argumento como inteiro.
    // Verifica que existe um proximo argumento e que ele e numerico. Mensagens de
    // erro mencionam o nome da flag para facilitar o diagnostico.
    private static int readIntValue(String[] args, int index, String name) {
        if (index + 1 >= args.length) {
            throw new IllegalArgumentException(name + " requires a value");
        }

        String raw = args[index + 1];

        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " requires an integer, got: " + raw);
        }
    }

    // Le o proximo argumento como string.
    // Verifica apenas que existe um proximo argumento. A validacao de conteudo
    // (por exemplo, string nao vazia) acontece no construtor.
    private static String readStringValue(String[] args, int index, String name) {
        if (index + 1 >= args.length) {
            throw new IllegalArgumentException(name + " requires a value");
        }

        return args[index + 1];
    }

    // Devolve o texto de uso do servidor.
    // Centraliza a documentacao das flags em um unico lugar para que o Main possa
    // imprimir esse texto quando o parsing falhar.
    public static String usage() {
        StringBuilder builder = new StringBuilder();

        builder.append("Usage: java -cp target\\classes com.joaopedro.miniredis.Main [options]").append(System.lineSeparator());
        builder.append("Options:").append(System.lineSeparator());
        builder.append("  --port <number>       TCP port (")
                .append(MIN_PORT).append("-").append(MAX_PORT)
                .append(", default ").append(DEFAULT_PORT).append(")").append(System.lineSeparator());
        builder.append("  --aof <path>          AOF file path (default ").append(DEFAULT_AOF_PATH).append(")").append(System.lineSeparator());
        builder.append("  --max-clients <n>     Max concurrent clients (>0, default ").append(DEFAULT_MAX_CLIENTS).append(")");

        return builder.toString();
    }
}
