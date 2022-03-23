package tar;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class Tar {
    private final String inputFileName;
    private final String outputFileName;
    private final List<String> inputFileNames;

    private static final char endSymbol = (char) 0;

    public Tar(String inputFileName, String outputFileName, List<String> inputFileNames) {
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        this.inputFileNames = inputFileNames;
    }

    public void start() {
        if (!inputFileName.isEmpty()) {
            startArchiving();
        }
    }

    private static String duplicateEndSymbols(String inputString) {
        StringBuilder line = new StringBuilder();
        for (char symbol : inputString.toCharArray()) {
            if (symbol == endSymbol)
                line.append(endSymbol);
            line.append(symbol);
        }
        return String.valueOf(line);
    }

    private void startArchiving() {
        FileWriter outputFile;
        // Открытие файла на запись
        try {
            outputFile = new FileWriter(outputFileName);
        } catch (IOException e) {
            System.err.println(outputFileName + " cannot be created (" + e.getMessage() + ")");
            System.err.println("Archiving stopped");
            return;
        }
        for (String file : inputFileNames)
            try (FileReader scanner = new FileReader(file)) {
                // Запись головного блока
                outputFile.write(String.valueOf(Path.of(file).getFileName()));
                outputFile.write("\n" + endSymbol + "\n");
                // Запись основного тела
                StringBuilder line = new StringBuilder();
                char symbol;
                while ((symbol = (char) scanner.read()) != (char) -1) {
                    line.append(symbol);
                    if (symbol == '\n') {
                        if (Pattern.matches("^" + endSymbol + "+\n$", String.valueOf(line)))
                            outputFile.write(duplicateEndSymbols(String.valueOf(line)));
                        else
                            outputFile.write(String.valueOf(line));
                        line = new StringBuilder();
                    }
                }
                if (Pattern.matches("^" + endSymbol + "$", String.valueOf(line)))
                    outputFile.write(duplicateEndSymbols(String.valueOf(line)));
                else
                    outputFile.write(String.valueOf(line));
                // Запись окончания
                outputFile.write("\n" + endSymbol + "\n");
            } catch (FileNotFoundException e) {
                System.err.println(file + " not found (" + e.getMessage() + ")");
            } catch (IOException e) {
                System.err.println(outputFileName + " cannot be changed (" + e.getMessage() + ")");
                System.err.println("Archiving stopped");
                return;
            }
        try {
            Objects.requireNonNull(outputFile).close();
        } catch (IOException e) {
            System.err.println(outputFileName + " cannot be closed (" + e.getMessage() + ")");
        }
    }
}

