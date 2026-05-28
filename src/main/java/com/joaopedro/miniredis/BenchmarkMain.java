package com.joaopedro.miniredis;

import com.joaopedro.miniredis.core.MiniRedis;

import java.util.Locale;

// Benchmark simples e educacional do MiniRedis.
//
// IMPORTANTE: esta classe NAO substitui ferramentas como o JMH. O objetivo e
// servir como uma medicao aproximada para comparar versoes do projeto entre
// si e ter uma nocao de ordem de grandeza das operacoes basicas. Os numeros
// dependem fortemente da maquina, JVM, carga do sistema e ate da temperatura
// do processador.
//
// O benchmark mede o nucleo do banco em memoria, sem TCP, sem AOF e sem o
// parser de comandos. O foco e a tabela hash manual e a logica de Entry.
public class BenchmarkMain {
    private static final int DEFAULT_OPERATIONS = 100_000;
    private static final int WARMUP_OPERATIONS = 10_000;

    // Sink usado para evitar que o JIT elimine chamadas sem efeito visivel.
    // E volatile e impresso no final para garantir que os retornos das operacoes
    // sejam considerados "usados" pelo otimizador.
    private static volatile long sink = 0;

    // Ponto de entrada do benchmark.
    // Resolve o numero de operacoes a partir dos argumentos, imprime o cabecalho,
    // roda o warmup e executa os 4 cenarios em sequencia.
    public static void main(String[] args) {
        int operations = resolveOperations(args);

        printHeader(operations);
        warmup();

        runSetBenchmark(operations);
        runGetBenchmark(operations);
        runDelBenchmark(operations);
        runMixBenchmark(operations);

        printFooter();
    }

    // Le o numero de operacoes a partir dos argumentos de linha de comando.
    // Usa o primeiro argumento se for um inteiro positivo, caso contrario aplica
    // o default e avisa no terminal.
    private static int resolveOperations(String[] args) {
        int result = DEFAULT_OPERATIONS;

        if (args.length >= 1) {
            try {
                int parsed = Integer.parseInt(args[0]);

                if (parsed > 0) {
                    result = parsed;
                } else {
                    System.out.println("Operations must be positive, using default " + DEFAULT_OPERATIONS);
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number '" + args[0] + "', using default " + DEFAULT_OPERATIONS);
            }
        }

        return result;
    }

    // Executa um warmup do JIT antes das medicoes reais.
    // Faz SET seguido de GET em uma instancia descartavel para que o compilador
    // JIT otimize os caminhos quentes antes dos cenarios medidos comecarem.
    private static void warmup() {
        System.out.println("Warmup: " + WARMUP_OPERATIONS + " operations...");

        MiniRedis redis = new MiniRedis();

        for (int i = 0; i < WARMUP_OPERATIONS; i++) {
            redis.set("warm" + i, "v");
        }

        for (int i = 0; i < WARMUP_OPERATIONS; i++) {
            String value = redis.get("warm" + i);

            if (value != null) {
                sink = sink + value.hashCode();
            }
        }

        System.out.println("Warmup done.");
        System.out.println();
    }

    // Mede o tempo de N operacoes SET em um banco vazio.
    // Pre-gera as chaves para que a concatenacao de strings nao entre na medicao.
    private static void runSetBenchmark(int operations) {
        MiniRedis redis = new MiniRedis();
        String[] keys = generateKeys(operations);

        long start = System.nanoTime();

        for (int i = 0; i < operations; i++) {
            String result = redis.set(keys[i], "value");

            sink = sink + result.hashCode();
        }

        long elapsed = System.nanoTime() - start;

        printResult("SET", operations, elapsed);
    }

    // Mede o tempo de N operacoes GET em um banco previamente populado.
    // Popula o banco fora da janela de medicao para que apenas o tempo do GET
    // seja contabilizado.
    private static void runGetBenchmark(int operations) {
        MiniRedis redis = new MiniRedis();
        String[] keys = generateKeys(operations);

        for (int i = 0; i < operations; i++) {
            redis.set(keys[i], "value");
        }

        long start = System.nanoTime();

        for (int i = 0; i < operations; i++) {
            String value = redis.get(keys[i]);

            if (value != null) {
                sink = sink + value.hashCode();
            }
        }

        long elapsed = System.nanoTime() - start;

        printResult("GET", operations, elapsed);
    }

    // Mede o tempo de N operacoes DEL em um banco previamente populado.
    // Cada chave existe no inicio do loop, entao todas as remocoes acertam um
    // valor real e seguem o caminho de remocao da tabela hash.
    private static void runDelBenchmark(int operations) {
        MiniRedis redis = new MiniRedis();
        String[] keys = generateKeys(operations);

        for (int i = 0; i < operations; i++) {
            redis.set(keys[i], "value");
        }

        long start = System.nanoTime();

        for (int i = 0; i < operations; i++) {
            int removed = redis.del(keys[i]);

            sink = sink + removed;
        }

        long elapsed = System.nanoTime() - start;

        printResult("DEL", operations, elapsed);
    }

    // Mede o tempo de uma carga mista 50/50 de SET e GET.
    // Indices pares fazem SET de uma nova chave, indices impares fazem GET da
    // chave gravada na iteracao anterior, garantindo que metade dos GETs sao hits.
    private static void runMixBenchmark(int operations) {
        MiniRedis redis = new MiniRedis();
        String[] keys = generateKeys(operations);

        long start = System.nanoTime();

        for (int i = 0; i < operations; i++) {
            if (i % 2 == 0) {
                String result = redis.set(keys[i], "value");

                sink = sink + result.hashCode();
            } else {
                String value = redis.get(keys[i - 1]);

                if (value != null) {
                    sink = sink + value.hashCode();
                }
            }
        }

        long elapsed = System.nanoTime() - start;

        printResult("MIX 50/50 SET+GET", operations, elapsed);
    }

    // Gera um array de chaves no formato "k<i>".
    // Faz o trabalho de construcao de strings antes do benchmark para nao poluir
    // a medicao com alocacao e concatenacao.
    private static String[] generateKeys(int count) {
        String[] keys = new String[count];

        for (int i = 0; i < count; i++) {
            keys[i] = "k" + i;
        }

        return keys;
    }

    // Imprime o resultado de um cenario.
    // Converte nanossegundos para milissegundos e calcula operacoes por segundo
    // como ops divido pelo tempo em segundos. Forca Locale.ROOT no format para
    // garantir ponto decimal independente do locale do sistema.
    private static void printResult(String label, int operations, long elapsedNanos) {
        double millis = elapsedNanos / 1_000_000.0;
        double seconds = elapsedNanos / 1_000_000_000.0;
        double opsPerSecond = operations / seconds;

        System.out.println(String.format(
                Locale.ROOT,
                "[%s] %d ops in %.2f ms = %.0f ops/sec",
                label, operations, millis, opsPerSecond));
    }

    // Imprime o cabecalho do benchmark.
    // Sempre exibe o aviso de que esta medicao nao substitui ferramentas como o
    // JMH, reforcando que os numeros sao apenas aproximados.
    private static void printHeader(int operations) {
        System.out.println("========================================================");
        System.out.println("Mini Redis - Simple Benchmark");
        System.out.println("========================================================");
        System.out.println("This is NOT JMH. Numbers are rough and machine-dependent.");
        System.out.println("Use for relative comparison, not as a definitive measure.");
        System.out.println("--------------------------------------------------------");
        System.out.println("Operations per scenario: " + operations);
        System.out.println("Warmup operations:       " + WARMUP_OPERATIONS);
        System.out.println("========================================================");
        System.out.println();
    }

    // Imprime o rodape do benchmark.
    // Tambem imprime o sink para garantir que o JIT nao remova o trabalho feito
    // dentro dos loops por considera-lo sem efeito observavel.
    private static void printFooter() {
        System.out.println();
        System.out.println("========================================================");
        System.out.println("Done. sink=" + sink);
        System.out.println("========================================================");
    }
}
