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
            String pathOfMainDir = Path.of("").toAbsolutePath().toString();
            for (String file : fileNamesForArchiving) {
                System.out.println("Archiving " + file + " started");
                try (InputStream inputFile = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE)) {
                    // Имя текущего входного файла
                    String filePath = Path.of(file).toAbsolutePath().toString();
                    // Проверка, что файл находится в поддиректории
                    if (!filePath.startsWith(pathOfMainDir))
                        throw new IllegalArgumentException("Incorrect path");
                    // Получение относительного пути
                    String fileName = filePath.replace(pathOfMainDir, "");
                    if (fileName.startsWith("\\") || fileName.startsWith("/")) fileName = fileName.substring(1);
                    // Проход по файлу
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead = inputFile.read(buffer);
                    writeNewPart(outputFile, fileName, buffer, bytesRead != -1 ? bytesRead : 0);
                    while ((bytesRead = inputFile.read(buffer)) != -1) {
                        writeNextPart(outputFile, buffer, bytesRead);
                    }
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

    private static String byteArrayToString(byte[] array, int size) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < size; i++)
            out.append((char) array[i]);
        return out.toString();
    }

    private static int towByteToInt(byte[] num) {
        return ((num[0] >= 0 ? num[0] : num[0] + 256) << 8) + (num[1] >= 0 ? num[1] : num[1] + 256);
    }

    private void startUnarchive() {
        // Открытие файла на чтение
        try (InputStream inputFile = new BufferedInputStream(new FileInputStream(fileNameToUnarchive))) {
            try {
                String fileName = "";
                byte[] buffer = new byte[3];
                if (inputFile.read(buffer) != 3) throw new IllegalArgumentException();
                do {
                    int countBytes = towByteToInt(buffer);
                    if (buffer[2] == (byte) 0xFF) {
                        int lengthFileName = (inputFile.read() << 8) + inputFile.read();
                        buffer = new byte[lengthFileName];
                        if (inputFile.read(buffer) != lengthFileName) throw new IllegalArgumentException();
                        fileName = byteArrayToString(buffer, lengthFileName);
                        Files.createDirectories(Path.of(fileName).getParent());
                    }
                    buffer = new byte[countBytes];
                    if (inputFile.read(buffer) != countBytes) throw new IllegalArgumentException();
                    try (OutputStream writer = new BufferedOutputStream(new FileOutputStream(fileName, true))) {
                        writer.write(buffer);
                    } catch (IOException e) {
                        System.err.println(fileName + " cannot be created or changed: " + e.getMessage());
                        System.err.println("Unzipping stopped");
                        return;
                    }
                    buffer = new byte[3];
                } while (inputFile.read(buffer) != -1);
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