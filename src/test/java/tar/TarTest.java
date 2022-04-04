package tar;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TarTest {

    private void randomFile(String fileName) {
        try {
            BufferedWriter file = new BufferedWriter(new FileWriter(fileName));
            int countLines = (int) (Math.random() * 999) + 1; // Число от 0 до 999
            for (int i = 0; i < countLines; i++) {
                int countSymbols = (int) (Math.random() * 1000);
                for (int j = 0; j < countSymbols; j++)
                    file.write((int) (Math.random() * (127 - 32) + 32));
                file.write("\n");
            }
            file.close();
        } catch (IOException e) {
            System.err.println("Fail with tests");
        }
    }

    private boolean assertFilesContent(String file1, String file2) {
        try {
            BufferedReader reader1 = new BufferedReader(new FileReader(file1));
            BufferedReader reader2 = new BufferedReader(new FileReader(file2));
            String reader1Line = reader1.readLine();
            String reader2Line = reader2.readLine();
            while (reader1Line != null && reader2Line != null) {
                if (!Objects.equals(reader1Line, reader2Line)) {
                    reader1.close();
                    reader2.close();
                    return false;
                }
                reader1Line = reader1.readLine();
                reader2Line = reader2.readLine();
            }
            reader1.close();
            reader2.close();
            return true;
        } catch (IOException e) {
            System.err.println("Fail with tests");
        }
        return false;
    }

    @Test
    public void testRandom() {
        new File("output").mkdir();
        new File("input").mkdir();
        for (int j = 0; j < 10; j++) {
            int countFiles = (int) (Math.random() * 99) + 1;
            List<String> files = new ArrayList<>();
            for (int i = 0; i < countFiles; i++) {
                randomFile("input/text" + i + ".txt");
                files.add("input/text" + i + ".txt");
            }
            new Tar("", "output/out.txt", files).start();
            new Tar("output/out.txt", "", null).start();
            for (int i = 0; i < countFiles; i++) {
                Assertions.assertTrue(assertFilesContent("input/text" + i + ".txt", "text" + i + ".txt"));
                new File("input/text" + i + ".txt").delete();
                new File("text" + i + ".txt").delete();
            }
            new File("output/out.txt").delete();
        }
    }

    @Test
    public void testVoidFiles() {
        try {
            new File("input").mkdir();
            new FileWriter("input/void1.txt").close();
            new FileWriter("input/void2.txt").close();
            FileWriter notVoid = new FileWriter("input/notVoid.txt");
            notVoid.write("asdads\nasdasdasd\nasdasdasdasdasd\n");
            notVoid.close();
            List<String> files = List.of("input/void1.txt", "input/void2.txt", "input/notVoid.txt");
            new Tar("", "out.txt", files).start();
            new Tar("out.txt", "", null).start();
            for (String file: files) {
                Assertions.assertTrue(assertFilesContent(file, Path.of(file).getFileName().toString()));
                new File(file).delete();
                new File(Path.of(file).getFileName().toString()).delete();
            }
            new File("out.txt").delete();
        } catch (IOException e) {
            System.err.println("Fail with tests");
        }
    }
}
