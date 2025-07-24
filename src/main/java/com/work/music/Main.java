package com.work.music;

/**
 *
 * @author ajuar
 */
public class Main {

    public static void main(String[] args) {
        // Verificar argumentos
        if (args.length != 2) {
            System.err.println("Uso: java AudioConverter <archivo_entrada> <archivo_salida>");
            System.err.println("Ejemplo: java -jar mvn-audio-converter-1.0.0-launcher.jar input.wav output.mp3");
            System.exit(1);
        }
        AudioConverter.converter(args[0], args[1]);
    }
}
