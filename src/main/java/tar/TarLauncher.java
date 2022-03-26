package tar;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TarLauncher {
    @Option(name = "-u", metaVar = "filename.txt", usage = "File name with merged files", forbids = {"-out"})
    private String inputFileName = "";
    @Option(name = "-out", metaVar = "output.txt", usage = "File name of merged files", forbids = {"-u"})
    private String outputFileName = "";
    @Argument(metaVar = "file1.txt file2.txt...", usage = "File names to merge")
    private List<String> inputFileNames = new ArrayList<>();

    public static void main(String[] args) {
        new TarLauncher().launch(args);
    }

    private void launch(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            if (outputFileName.isEmpty() && !inputFileNames.isEmpty())
                throw new IllegalArgumentException("option \"-u\" does not require arguments");
            if (inputFileName.isEmpty() && inputFileNames.stream().noneMatch(it -> new File(it).exists()))
                throw new IllegalArgumentException("option \"-out\" require arguments" +
                        (!inputFileNames.isEmpty() ? " (" + inputFileNames + " not found)" : ""));
        } catch (CmdLineException | IllegalArgumentException e) {
            System.err.println(e.getMessage());
            // input/text.txt input/text2.txt input/text3.txt txt -out out.txt
            System.err.println("java -jar tar.jar [-u filename.txt | file1.txt file2.txt... -out output.txt]");
            parser.printUsage(System.err);
            return;
        }
        Tar tar = new Tar(inputFileName, outputFileName, inputFileNames);
        tar.start();
    }
}
