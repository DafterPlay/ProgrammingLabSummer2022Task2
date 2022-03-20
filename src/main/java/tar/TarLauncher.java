package tar;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

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
            if (inputFileName.isEmpty() && inputFileNames.isEmpty())
                throw new IllegalArgumentException("option \"-out\" require arguments");
        } catch (CmdLineException | IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("java -jar tar.jar [-u filename.txt | file1.txt file2.txt... -out output.txt]");
            parser.printUsage(System.err);
            return;
        }
    }
}
