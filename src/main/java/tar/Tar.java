package tar;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class Tar {
    private final String fileNameToUnarchive;
    private final String nameOfOutputArchive;
    private final List<String> fileNamesForArchiving;

    private static final int COUNT_LINES = 10;

    public Tar(String fileNameToUnarchive, String nameOfOutputArchive, List<String> fileNamesForArchiving) {
        this.fileNameToUnarchive = fileNameToUnarchive;
        this.nameOfOutputArchive = nameOfOutputArchive;
        this.fileNamesForArchiving = fileNamesForArchiving;
    }

    public void start() {
        if (!nameOfOutputArchive.isEmpty()) {
            startArchiving();
        } else {
            startUnarchive();
        }
    }

    private void writeNextPart(BufferedWriter outputFile, String fileName, List<String> buffer) throws IOException {
        writeNextPart(outputFile, fileName, buffer, COUNT_LINES);
    }

    private void writeNextPart(BufferedWriter outputFile, String fileName, List<String> buffer, int countLines) throws IOException {
        // Запись головного блока
        outputFile.write(fileName);
        outputFile.write("\n" + countLines + "\n");
        // Запись тела
        for (String s : buffer)
            outputFile.write(s);
    }

    private void startArchiving() {
        BufferedWriter outputFile;
        // Открытие файла на запись
        try {
            outputFile = new BufferedWriter(new FileWriter(nameOfOutputArchive));
        } catch (IOException e) {
            System.err.println(nameOfOutputArchive + " cannot be created: " + e.getMessage());
            System.err.println("Archiving stopped");
            return;
        }
        // Перебор всех файлов
        for (String file : fileNamesForArchiving) {
            System.out.println("Archiving " + file + " started");
            try (BufferedReader inputFile = new BufferedReader(new FileReader(file))) {
                boolean inputFileIsEmpty = true;
                // Подсчитывает число строк
                int counter = 0;
                // Имя текущего входного файла
                String fileName = Path.of(file).getFileName().toString();
                StringBuilder line = new StringBuilder();
                // Проход по файлу
                char symbol;
                List<String> buffer = new ArrayList<>();
                while ((symbol = (char) inputFile.read()) != (char) -1) {
                    inputFileIsEmpty = false;
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
                // Если мы не записали строку в буфер
                if (line.length() != 0)
                    buffer.add(line.toString());
                if (!buffer.isEmpty()) {
                    // Записать оставшиеся в буфере данные
                    writeNextPart(outputFile, fileName, buffer, buffer.size() +
                            (buffer.get(buffer.size() - 1).contains("\n") ? 1 : 0));
                    outputFile.write("\n");
                }
                // Если файл оказался пустой, записать одну пустую строку
                if (inputFileIsEmpty) {
                    writeNextPart(outputFile, fileName, new ArrayList<>(), 1);
                    outputFile.write("\n");
                }
            } catch (FileNotFoundException e) {
                System.err.println(file + " not found: " + e.getMessage());
                continue;
            } catch (IOException e) {
                System.err.println(nameOfOutputArchive + " cannot be changed: " + e.getMessage());
                System.err.println("Archiving stopped");
                return;
            }
            System.out.println("Archiving " + file + " ended");

        }
        try {
            Objects.requireNonNull(outputFile).close();
        } catch (IOException e) {
            System.err.println(nameOfOutputArchive + " cannot be closed: " + e.getMessage());
        }
        System.out.println("Archiving complete, output file: " + nameOfOutputArchive);
    }

    private void startUnarchive() {
        BufferedReader inputFile;
        // Открытие файла на чтение
        try {
            inputFile = new BufferedReader(new FileReader(fileNameToUnarchive));
        } catch (FileNotFoundException e) {
            System.err.println(fileNameToUnarchive + " not found: " + e.getMessage());
            System.err.println("Unzipping stopped");
            return;
        }
        try {
            String fileName = inputFile.readLine();
            int countLines;
            while (fileName != null) {
                BufferedWriter writer;
                try {
                    writer = new BufferedWriter(new FileWriter(fileName, true));
                } catch (IOException e) {
                    System.err.println(fileName + " cannot be created or changed: " + e.getMessage());
                    System.err.println("Unzipping stopped");
                    return;
                }
                countLines = Integer.parseInt(inputFile.readLine());
                for (int i = 0; i < countLines; i++) {
                    String line = inputFile.readLine();
                    try {
                        writer.append(line);
                        if (i != countLines - 1)
                            writer.append("\n");
                        else {
                            String newFileName = inputFile.readLine();
                            if (newFileName != null && newFileName.equals(fileName))
                                writer.append("\n");
                            fileName = newFileName;
                        }
                    } catch (IOException e) {
                        System.err.println(fileName + " cannot be changed: " + e.getMessage());
                        System.err.println("Unzipping stopped");
                        return;
                    }
                }
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println(fileName + " cannot be closed: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println(fileNameToUnarchive + " cannot be read: " + e.getMessage());
            System.err.println("Unzipping stopped");
            return;
        } catch (NumberFormatException e) {
            System.err.println("Incorrect format input file (" + fileNameToUnarchive + ")");
            System.err.println("Unzipping stopped");
            return;
        }
        try {
            Objects.requireNonNull(inputFile).close();
        } catch (IOException e) {
            System.err.println(fileNameToUnarchive + " cannot be closed: " + e.getMessage());
        }
    }
}