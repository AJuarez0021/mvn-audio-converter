package com.work.music;

import com.work.music.util.ConsoleHelper;
import ws.schild.jave.*;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.progress.EncoderProgressListener;
import ws.schild.jave.info.MultimediaInfo;
import java.io.File;

/**
 *
 * @author ajuar
 */
public class AudioConverter {

    // Clase para manejar el progreso de la conversión
    private static class ProgressListener implements EncoderProgressListener {

        private long totalDuration = 0;
        private long lastUpdateTime = 0;
        private static final long UPDATE_INTERVAL = 500; // Actualizar cada 500ms

        public ProgressListener(long totalDuration) {
            this.totalDuration = totalDuration;
        }

        @Override
        public void sourceInfo(MultimediaInfo info) {
            // Se llama cuando se obtiene información del archivo fuente
            if (info.getDuration() > 0) {
                this.totalDuration = info.getDuration();
            }
            ConsoleHelper.println("\nInformacion del archivo:");
            ConsoleHelper.println("Duracion: " + formatDuration(info.getDuration()));
            if (info.getAudio() != null) {
                ConsoleHelper.println("Bitrate: " + info.getAudio().getBitRate() + " bps");
                ConsoleHelper.println("Canales: " + info.getAudio().getChannels());
                ConsoleHelper.println("Sampling Rate: " + info.getAudio().getSamplingRate() + " Hz");
            }
            ConsoleHelper.println();
        }

        @Override
        public void progress(int permil) {
            // Se llama para reportar el progreso (permil = progreso en partes por mil)
            long currentTime = System.currentTimeMillis();

            // Actualizar solo cada cierto intervalo
            if (currentTime - lastUpdateTime >= UPDATE_INTERVAL || permil >= 1000) {
                lastUpdateTime = currentTime;

                double percentage = permil / 10.0; // Convertir de permil a porcentaje
                long processedDuration = (totalDuration * permil) / 1000;

                // Crear barra de progreso visual
                String progressBar = createProgressBar(percentage);

                // Limpiar línea anterior y mostrar progreso
                ConsoleHelper.print("\rProgreso: " + progressBar
                        + String.format(" %.1f%% ", percentage)
                        + "(" + formatDuration(processedDuration) + "/" + formatDuration(totalDuration) + ")");

                // Si está completo, agregar nueva línea
                if (permil >= 1000) {
                    ConsoleHelper.println();
                }
            }
        }

        @Override
        public void message(String message) {
            // Se llama cuando hay mensajes informativos del encoder
            if (message != null && !message.trim().isEmpty()) {
                ConsoleHelper.println("\nInfo: " + message);
            }
        }

        /**
         * Crea una barra de progreso visual
         */
        private String createProgressBar(double percentage) {
            int barLength = 30;
            int filled = (int) (barLength * percentage / 100.0);

            StringBuilder bar = new StringBuilder("[");
            for (int i = 0; i < barLength; i++) {
                if (i < filled) {
                    bar.append("=");
                } else if (i == filled && percentage % (100.0 / barLength) > 0) {
                    bar.append("==");
                } else {
                    bar.append("-");
                }
            }
            bar.append("]");
            return bar.toString();
        }

        /**
         * Formatea la duración en milisegundos a formato legible
         */
        private String formatDuration(long durationMs) {
            if (durationMs <= 0) {
                return "00:00";
            }

            long seconds = durationMs / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;

            seconds %= 60;
            minutes %= 60;

            if (hours > 0) {
                return String.format("%02d:%02d:%02d", hours, minutes, seconds);
            } else {
                return String.format("%02d:%02d", minutes, seconds);
            }
        }
    }
   
    public static void converter(String inputFileName, String outputFileName) {

        // Crear objetos File
        File inputFile = new File(inputFileName);
        File outputFile = new File(outputFileName);

        try {
            validateFile(inputFile);

            // Crear el convertidor
            Encoder encoder = new Encoder();

            // Obtener información del archivo de entrada para el listener
            MultimediaObject inputObject = new MultimediaObject(inputFile);

            long duration = getDuration(inputObject);

            // Detectar formato de salida basado en la extensión
            String outputExtension = getFileExtension(outputFileName).toLowerCase();

            // Configurar atributos de audio
            AudioAttributes audioAttributes = createAudioAttributes(outputExtension);

            // Configurar atributos de codificación
            EncodingAttributes encodingAttributes = createEncodingAttributes(outputExtension, audioAttributes);

            ConsoleHelper.println("Iniciando conversion de audio...");
            ConsoleHelper.println("Entrada: " + inputFileName);
            ConsoleHelper.println("Salida: " + outputFileName);
            ConsoleHelper.println("Formato: " + outputExtension.toUpperCase());

            // Crear y configurar el listener de progreso
            ProgressListener progressListener = new ProgressListener(duration);

            // Realizar la conversión con el listener
            long startTime = System.currentTimeMillis();
            encoder.encode(inputObject, outputFile, encodingAttributes, progressListener);
            long endTime = System.currentTimeMillis();

            ConsoleHelper.println("\nConversion completada exitosamente");
            ConsoleHelper.println("Tiempo transcurrido: " + formatTime(endTime - startTime));

            // Mostrar información de los archivos
            ConsoleHelper.println("\nInformacion de archivos:");
            ConsoleHelper.println("Archivo original: " + formatFileSize(inputFile.length()));
            ConsoleHelper.println("Archivo convertido: " + formatFileSize(outputFile.length()));

            // Calcular ratio de compresión
            if (inputFile.length() > 0) {
                double ratio = (double) outputFile.length() / inputFile.length();

                displayRatio(ratio);
            }

        } catch (EncoderException e) {
            System.err.println("\nError durante la conversion: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("\nError inesperado: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

     private static void validateFile(File inputFile) {
        // Verificar que el archivo de entrada existe
        if (!inputFile.exists()) {
            System.err.println("Error: El archivo de entrada '" + inputFile.getName() + "' no existe.");
            System.exit(1);
        }

        // Verificar que el archivo de entrada es un archivo (no directorio)
        if (!inputFile.isFile()) {
            System.err.println("Error: '" + inputFile.getName() + "' no es un archivo válido.");
            System.exit(1);
        }
    }
     
    private static void displayRatio(double ratio) {
        if (ratio < 1.0) {
            // Archivo más pequeño - mostrar reducción
            double reduction = (1 - ratio) * 100;
            ConsoleHelper.println("Ratio de compresion: " + String.format("%.2f", ratio)
                    + " (" + String.format("%.1f%% reduccion)", reduction));
        } else if (ratio > 1.0) {
            // Archivo más grande - mostrar incremento
            double increase = (ratio - 1) * 100;
            ConsoleHelper.println("Ratio de compresion: " + String.format("%.2f", ratio)
                    + " (" + String.format("%.1f%% incremento)", increase));
        } else {
            // Archivo del mismo tamaño
            ConsoleHelper.println("   Cambio de tamaño: 0.0% (sin cambio)");
        }
    }

    private static long getDuration(MultimediaObject inputObject) {
        long duration = 0;
        try {
            MultimediaInfo inputInfo = inputObject.getInfo();
            duration = inputInfo.getDuration();
        } catch (EncoderException e) {
            ConsoleHelper.println("No se pudo obtener la duracion del archivo, continuando...");
        }
        return duration;
    }

    private static AudioAttributes createAudioAttributes(String format) {
        AudioAttributes audioAttributes = new AudioAttributes();
        audioAttributes.setChannels(2); // Estéreo
        audioAttributes.setSamplingRate(44100); // 44.1 kHz

        //Configura los atributos de audio según el formato de salida
        switch (format) {
            case "mp3" -> {
                audioAttributes.setCodec("libmp3lame");
                audioAttributes.setBitRate(128000);
            }
            case "wav" -> {
                audioAttributes.setCodec("pcm_s16le");
                audioAttributes.setBitRate(null); // WAV no comprimido no usa bitrate
            }
            case "ogg" -> {
                audioAttributes.setCodec("libvorbis");
                audioAttributes.setBitRate(128000);
            }
            case "aac" -> {
                audioAttributes.setCodec("aac");
                audioAttributes.setBitRate(128000);
            }
            case "flac" -> {
                audioAttributes.setCodec("flac");
                audioAttributes.setBitRate(null); // FLAC es sin pérdida
            }
            case "wma" -> {
                audioAttributes.setCodec("wmav2");
                audioAttributes.setBitRate(128000);
            }           
            default -> {
                ConsoleHelper.println("Formato no reconocido, usando configuración por defecto (MP3)");
                audioAttributes.setCodec("libmp3lame");
                audioAttributes.setBitRate(128000);
            }
        }
        return audioAttributes;
    }

    private static EncodingAttributes createEncodingAttributes(String outputExtension, AudioAttributes audioAttributes) {
        EncodingAttributes encodingAttributes = new EncodingAttributes();
        encodingAttributes.setOutputFormat(outputExtension);
        encodingAttributes.setAudioAttributes(audioAttributes);
        return encodingAttributes;
    }

    /**
     * Formatea el tiempo en milisegundos a formato legible
     */
    private static String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds %= 60;

        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Obtiene la extensión de un archivo
     */
    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "mp3"; // Extensión por defecto
        }
        return fileName.substring(lastDotIndex + 1);
    }

    /**
     * Formatea el tamaño del archivo de manera legible
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
