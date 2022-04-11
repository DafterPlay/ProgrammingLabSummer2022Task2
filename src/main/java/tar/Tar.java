package tar;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Tar {
    private final String fileNameToUnarchive;
    private final String nameOfOutputArchive;
    private final List<String> fileNamesForArchiving;

    private static final int BUFFER_SIZE = 16384;

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

    // Записывает размер файла в 2 байта со смещением, для информации о наличии названия
    private static void writeNewPart(OutputStream outputFile, String fileName, byte[] buffer, int bufferSize) throws IOException {
        byte[] fileNameBytes = fileName.getBytes();
        if (fileNameBytes.length > 65535) throw new IllegalArgumentException("The file name is too long");
        // Записали длину следующего блока (не более 65520)
        outputFile.write(bufferSize >> 8);
        outputFile.write(bufferSize & 0x00FF);
        // Добавили информацию, что далее будет заголовок
        outputFile.write(0xFF);
        // Записали длину заголовка
        outputFile.write(fileNameBytes.length >> 8);
        outputFile.write(fileNameBytes.length & 0x00FF);
        // Записали само имя
        outputFile.write(fileNameBytes);
        // Записали основной блок информации
        outputFile.write(buffer, 0, bufferSize);
    }

    private static void writeNextPart(OutputStream outputFile, byte[] buffer, int bufferSize) throws IOException {
        // Записали длину следующего блока (не более 65520)
        outputFile.write(bufferSize >> 8);
        outputFile.write(bufferSize & 0x00FF);
        outputFile.write(0x00);
        // Записали основной блок информации
        outputFile.write(buffer, 0, bufferSize);
    }

    private void startArchiving() {
        // Открытие файла на запись
        try (OutputStream outputFile = new BufferedOutputStream(new FileOutputStream(nameOfOutputArchive), BUFFER_SIZE)) {
            // Перебор всех файлов
            Path pathOfMainDir = Path.of("").toAbsolutePath();
            for (String file : fileNamesForArchiving) {
                System.out.println("Archiving " + file + " started");
                try (InputStream inputFile = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE)) {
                    // Имя текущего входного файла
                    Path filePath = Path.of(file).toAbsolutePath();
                    // Проверка, что файл находится в поддиректории
                    if (!filePath.startsWith(pathOfMainDir))
                        throw new IllegalArgumentException("Incorrect path");
                    // Получение относительного пути
                    String fileName = pathOfMainDir.relativize(filePath).toString();
                    // Проход по файлу
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead = inputFile.read(buffer);
                    writeNewPart(outputFile, fileName, buffer, bytesRead != -1 ? bytesRead : 0);
                    while ((bytesRead = inputFile.read(buffer)) != -1)
                        writeNextPart(outputFile, buffer, bytesRead);
                } catch (IllegalArgumentException e) {
                    System.err.println(file + " error: " + e.getMessage());
                    continue;
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
            System.out.println("Archiving complete, output file: " + nameOfOutputArchive);
        } catch (IOException e) {
            System.err.println(nameOfOutputArchive + " exception: " + e.getMessage());
            System.err.println("Archiving stopped");
        }
    }

    private static int getTwoBytes(InputStream inputFile) throws IOException {
        int a = inputFile.read();
        if (a == -1) return -1;
        return (a << 8) + inputFile.read();
    }

    private static int getOneByte(InputStream inputFile) throws IOException {
        int a = inputFile.read();
        if (a == -1) throw new IllegalArgumentException();
        return a;
    }

    private String getNameForFile(InputStream inputFile) throws IOException {
        int lengthFileName = (inputFile.read() << 8) + inputFile.read();
        byte[] bufferFileName = new byte[lengthFileName];
        if (inputFile.read(bufferFileName) != lengthFileName) throw new IllegalArgumentException();
        String fileName = new String(bufferFileName);
        try {
            Files.createDirectories(Path.of(fileName).getParent());
        } catch (IOException e) {
            throw new FileNotFoundException(fileName + " cannot be created");
        }
        return fileName;
    }

    private void startUnarchive() {
        System.out.println("Unzipping started");
        try (InputStream inputFile = new BufferedInputStream(new FileInputStream(fileNameToUnarchive))) {
            try {
                Path fileName = Path.of("");
                int countBytes;
                OutputStream outputFile = null;
                while ((countBytes = getTwoBytes(inputFile)) != -1) {
                    if (getOneByte(inputFile) == 0xFF) {
                        if (outputFile != null)
                            outputFile.close();
                        outputFile = new BufferedOutputStream(new FileOutputStream(getNameForFile(inputFile)), BUFFER_SIZE);
                    }
                    byte[] buffer = new byte[countBytes];
                    if (inputFile.read(buffer) != countBytes) throw new IllegalArgumentException();
                    try {
                        if (outputFile == null) throw new IllegalArgumentException();
                        outputFile.write(buffer);
                    } catch (IOException e) {
                        System.err.println(fileName + " cannot be created or changed: " + e.getMessage());
                        System.err.println("Unzipping stopped");
                        return;
                    }
                }
                if (outputFile != null)
                    outputFile.close();
                System.out.println("Unzipping completed");
            } catch (FileNotFoundException e) {
                System.err.println(e.getMessage());
                System.err.println("Unzipping stopped");
            } catch (IOException e) {
                System.err.println(fileNameToUnarchive + " cannot be read: " + e.getMessage());
                System.err.println("Unzipping stopped");
            } catch (IllegalArgumentException e) {
                System.err.println("Incorrect format input file (" + fileNameToUnarchive + ")");
                System.err.println("Unzipping stopped");
            }
        } catch (IOException e) {
            System.err.println(fileNameToUnarchive + " not found: " + e.getMessage());
            System.err.println("Unzipping stopped");
        }
    }
}