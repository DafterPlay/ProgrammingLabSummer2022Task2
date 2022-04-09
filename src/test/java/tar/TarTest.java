package tar;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class TarTest {
    private boolean assertFilesContent(String fileName1, String fileName2) {
        int BUFFER_SIZE = 4096;
        try (
                InputStream file1 = new BufferedInputStream(new FileInputStream(fileName1), BUFFER_SIZE);
                InputStream file2 = new BufferedInputStream(new FileInputStream(fileName2), BUFFER_SIZE)
        ) {
            byte[] buffer1 = new byte[BUFFER_SIZE],
                    buffer2 = new byte[BUFFER_SIZE];
            while (file1.read(buffer1) != -1 && file2.read(buffer2) != -1)
                if (!Arrays.equals(buffer1, buffer2)) return false;
        } catch (IOException e) {
            System.err.println("Fail with tests");
            return false;
        }
        return true;
    }

    private void randomFile(String fileName) {
        try (OutputStream file = new BufferedOutputStream(new FileOutputStream(fileName))) {
            int countBytes = (int) (Math.random() * 1048576); // 0 - 1048576 байт (1 Мб)
            for (int i = 0; i < countBytes; i++)
                file.write((int) (Math.random() * 255));
        } catch (IOException e) {
            System.err.println("Fail with tests");
        }
    }

    private String randomFileName() {
        Set<Integer> illegalChars = Set.of(34, 42, 47, 58, 60, 62, 63, 92, 124);
        int size = (int) (Math.random() * 50 + 50);
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < size; i++) {
            int newSymbol = (int) (Math.random() * 223 + 32);
            while (illegalChars.contains(newSymbol)) {
                newSymbol = (int) (Math.random() * 223 + 32);
            }
            name.append((char) newSymbol);
        }
        return name + ".txt";
    }

    private void createDir(String name) {
        if (!(new File(name).mkdir())) throw new IllegalArgumentException("Dir " + name + " cannot be created");
    }

    private void removeDirOrFile(String name) {
        if (!(new File(name).delete())) throw new IllegalArgumentException(name + " cannot be removed");
    }

    private void renameDir(String currentName, String newName) {
        if (!(new File(currentName).renameTo(new File(newName))))
            throw new IllegalArgumentException("Dir " + currentName + " cannot be renamed to " + newName);
    }

    private void assertFilesTrue(String input, String checkDir, List<String> files) {
        for (String file : files) {
            assertTrue(assertFilesContent(input + "/" + file, checkDir + "/" + file));
            removeDirOrFile(input + "/" + file);
            removeDirOrFile(checkDir + "/" + file);
        }
    }

    @Test
    public void testRandom() {
        String inputDir = "input";
        for (int j = 0; j < 10; j++) {
            createDir(inputDir);
            int countFiles = (int) (Math.random() * 99) + 1;
            List<String> files = new LinkedList<>();
            for (int i = 0; i < countFiles; i++) {
                files.add(randomFileName());
                randomFile(inputDir + "/" + files.get(files.size() - 1));
            }
            new Tar("", "out.txt",
                    files.stream().map(it -> inputDir + "/" + it).collect(Collectors.toList())).start();
            String checkDir = "check" + System.currentTimeMillis();
            renameDir(inputDir, checkDir);
            new Tar("out.txt", "", null).start();
            assertFilesTrue(inputDir, checkDir, files);
            removeDirOrFile("out.txt");
            removeDirOrFile(checkDir);
            removeDirOrFile(inputDir);
        }
    }

    @Test
    public void testVoidFiles() {
        String inputDir = "input";
        createDir(inputDir);
        List<String> files = new LinkedList<>();
        try {
            files.add(randomFileName());
            Files.writeString(Path.of(inputDir + "/" + files.get(files.size() - 1)), "");
            files.add(randomFileName());
            Files.writeString(Path.of(inputDir + "/" + files.get(files.size() - 1)), "");
            files.add(randomFileName());
            Files.writeString(Path.of(inputDir + "/" + files.get(files.size() - 1)), "asdads\nasdasdasd\nasdasdasdasdasd\n");
            new Tar("", "out.txt",
                    files.stream().map(it -> inputDir + "/" + it).collect(Collectors.toList())).start();
            String checkDir = "check";
            renameDir(inputDir, checkDir);
            new Tar("out.txt", "", null).start();
            assertFilesTrue(inputDir, checkDir, files);
        } catch (IOException e) {
            System.err.println("Fail with tests");
        }
        removeDirOrFile("out.txt");
        removeDirOrFile("check");
        removeDirOrFile("input");
    }

    @Test
    public void test() {
        String inputDir = "input";
        createDir(inputDir);
        List<String> files = new LinkedList<>();
        try {
            files.add(randomFileName());
            Files.writeString(Path.of(inputDir + "/" + files.get(files.size() - 1)), "asdads\nasdasdasd\nasdasdasdasdasd");
            files.add(randomFileName());
            Files.writeString(Path.of(inputDir + "/" + files.get(files.size() - 1)), "asdads\nasdasdasd\nasdasasdasddasdasdasd\n");
            new Tar("", "out.txt",
                    files.stream().map(it -> inputDir + "/" + it).collect(Collectors.toList())).start();
            String checkDir = "check";
            renameDir(inputDir, checkDir);
            new Tar("out.txt", "", null).start();
            assertFilesTrue(inputDir, checkDir, files);
        } catch (IOException e) {
            System.err.println("Fail with tests");
        }
        removeDirOrFile("out.txt");
        removeDirOrFile("check");
        removeDirOrFile("input");
    }
}
