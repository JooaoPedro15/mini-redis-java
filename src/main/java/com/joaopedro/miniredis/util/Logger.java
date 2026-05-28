package com.joaopedro.miniredis.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String component;

    // Cria um logger associado a um componente.
    // O nome do componente aparece em cada linha de log para facilitar identificar
    // a origem das mensagens em saidas de varios modulos.
    public Logger(String component) {
        this.component = component;
    }

    // Registra uma mensagem de nivel INFO.
    // Usa stdout porque INFO representa eventos normais do servidor e nao precisa
    // ser separado para alertas.
    public void info(String message) {
        log("INFO ", message, System.out);
    }

    // Registra uma mensagem de nivel WARN.
    // Usa stderr para que alertas possam ser filtrados ou redirecionados de forma
    // independente da saida normal.
    public void warn(String message) {
        log("WARN ", message, System.err);
    }

    // Registra uma mensagem de nivel ERROR.
    // Usa stderr porque indica falha. Mantem o mesmo formato dos outros niveis para
    // uniformidade do log.
    public void error(String message) {
        log("ERROR", message, System.err);
    }

    // Formata e imprime uma linha de log.
    // Monta a string com timestamp, nivel, componente e mensagem e envia para o
    // PrintStream recebido. PrintStream.println e sincronizado internamente, entao
    // chamadas concorrentes nao intercalam dentro de uma linha.
    private void log(String level, String message, java.io.PrintStream out) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        out.println("[" + timestamp + "] [" + level + "] [" + component + "] " + message);
    }
}
