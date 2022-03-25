package tar;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class Tar {
    private final String inputFileName;
    private final String outputFileName;
    private final List<String> inputFileNames;

    private static final int COUNT_LINES = 10;
    private static final char END_SYMBOL = (char) 0;

    public Tar(String inputFileName, String outputFileName, List<String> inputFileNames) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.inputFileNames = inputFileNames;
    }

    public void start() {
        if (!outputFileName.isEmpty()) {
            startArchiving();
        }
    }

    private void writeNextPart(FileWriter outputFile, String fileName, List<String> buffer) throws IOException {
        writeNextPart(outputFile, fileName, buffer, COUNT_LINES);
    }

    private void writeNextPart(FileWriter outputFile, String fileName, List<String> buffer, int countLines) throws IOException {
        // Запись головного блока
        outputFile.write(END_SYMBOL + "\n");
        outputFile.write(fileName);
        outputFile.write("\n" + countLines + "\n" + END_SYMBOL + "\n");
        // Запись тела
        for (String s : buffer)
            outputFile.write(s);
        // Запись конца
        outputFile.write((buffer.size() - (buffer.get(buffer.size() - 1).contains("\n") ? 1 : 0) == COUNT_LINES
                ? "" : "\n") + END_SYMBOL + "\n");
    }

    private void startArchiving() {
        FileWriter outputFile;
        // Открытие файла на запись
        try {
            outputFile = new FileWriter(outputFileName);
        } catch (IOException e) {
            System.err.println(outputFileName + " cannot be created: " + e.getMessage());
            System.err.println("Archiving stopped");
            return;
        }
        // Перебор всех файлов
        for (String file : inputFileNames)
            try (BufferedReader scanner = new BufferedReader(new FileReader(file))) {
                // Подсчитывает число строк
                int counter = 0;
                // Имя текущего входного файла
                String fileName = String.valueOf(Path.of(file).getFileName());
                StringBuilder line = new StringBuilder();
                // Проход по файлу
                char symbol;
                List<String> buffer = new ArrayList<>();
                while ((symbol = (char) scanner.read()) != (char) -1) {
                    line.append(symbol);
                    if (symbol == '\n') {
                        if (counter == COUNT_LINES) {
                            // Записывает порцию данных
                            writeNextPart(outputFile, fileName, buffer);
                            buffer.clear();
                            counter = 0;
                        }
                        // Запись значений в буфер
                        buffer.add(line.toString());
                        line = new StringBuilder();
                        counter++;
                    }
                }
                if (!line.toString().isEmpty())
                    buffer.add(line.toString());
                if (!buffer.isEmpty()) {
                    writeNextPart(outputFile, fileName, buffer, buffer.size() +
                            (buffer.get(buffer.size() - 1).contains("\n") ? 1 : 0));
                }
            } catch (FileNotFoundException e) {
                System.err.println(file + " not found: " + e.getMessage());
            } catch (IOException e) {
                System.err.println(outputFileName + " cannot be changed: " + e.getMessage());
                System.err.println("Archiving stopped");
                return;
            }
        try {
            Objects.requireNonNull(outputFile).close();
        } catch (IOException e) {
            System.err.println(outputFileName + " cannot be closed: " + e.getMessage());
        }
    }
}