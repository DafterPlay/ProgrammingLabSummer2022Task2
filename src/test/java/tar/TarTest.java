package tar;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class TarTest {
    private void randomFile(String fileName) {
        try (OutputStream file = new BufferedOutputStream(new FileOutputStream(fileName))) {
            int countBytes = (int) (Math.random() * 1048576); // 0 - 1048576 байт (1 Мб)
            for (int i = 0; i < countBytes; i++)
                file.write((int) (Math.random() * 255));
        } catch (IOException e) {
            System.err.println("Fail with tests");
        }
    }

    private boolean assertFilesContent(String fileName1, String fileName2) {
        int BUFFER_SIZE = 1024;
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

    @Test
    public void testRandom() {
        createDir("output");
        createDir("input");
        for (int j = 0; j < 10; j++) {
            int countFiles = (int) (Math.random() * 99) + 1;
            List<String> files = new LinkedList<>();
            for (int i = 0; i < countFiles; i++) {
                randomFile("input/text" + i + ".txt");
                files.add("input/text" + i + ".txt");
            }
            new Tar("", "output/out.txt", files).start();
            renameDir("input", "check");
            new Tar("output/out.txt", "", null).start();
            for (int i = 0; i < countFiles; i++) {
                assertTrue(assertFilesContent("input/text" + i + ".txt", "check/text" + i + ".txt"));
                removeDirOrFile("input/text" + i + ".txt");
                removeDirOrFile("check/text" + i + ".txt");
            }
            removeDirOrFile("output/out.txt");
            removeDirOrFile("check");
        }
        removeDirOrFile("output");
        removeDirOrFile("input");
    }

    @Test
    public void testVoidFiles() {
        try {
            createDir("input");
            new FileWriter("input/void1.txt").close();
            new FileWriter("input/void2.txt").close();
            FileWriter notVoid = new FileWriter("input/notVoid.txt");
            notVoid.write("asdads\nasdasdasd\nasdasdasdasdasd\n");
            notVoid.close();
            List<String> files = List.of("input/void1.txt", "input/void2.txt", "input/notVoid.txt");
            new Tar("", "out.txt", files).start();
            renameDir("input", "check");
            new Tar("out.txt", "", null).start();
            for (String file : files) {
                assertTrue(assertFilesContent(file, "check/" + Path.of(file).getFileName().toString()));
                removeDirOrFile(file);
                removeDirOrFile("check/" + Path.of(file).getFileName().toString());
            }
            removeDirOrFile("out.txt");
        } catch (IOException e) {
            System.err.println("Fail with tests");
        }
        removeDirOrFile("check");
        removeDirOrFile("input");
    }

    @Test
    public void test() {
        try {
            createDir("input");
            FileWriter file1 = new FileWriter("input/file.txt");
            file1.write("asdads\nasdasdasd\nasdasdasdasdasd\n");
            file1.close();
            file1 = new FileWriter("input/file1.txt");
            file1.write("asdads\nasdasdasd\nasdasasdasddasdasdasd\n");
            file1.close();
            List<String> files = List.of("input/file.txt", "input/file1.txt");
            new Tar("", "out.txt", files).start();
            renameDir("input", "check");
            new Tar("out.txt", "", null).start();
            for (String file : files) {
                assertTrue(assertFilesContent(file, "check/" + Path.of(file).getFileName().toString()));
                removeDirOrFile(file);
                removeDirOrFile("check/" + Path.of(file).getFileName().toString());
            }
            removeDirOrFile("out.txt");
        } catch (IOException e) {
            System.err.println("Fail with tests");
        }
        removeDirOrFile("check");
        removeDirOrFile("input");
    }
}
