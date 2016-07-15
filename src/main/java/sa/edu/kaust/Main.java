import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.file.*;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.JCommander;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Main {

    public static final String PROJECT_PROPERTIES = "project.properties";
    Logger log = Logger.getLogger(Main.class.getName());
    Properties props;
    PhenoSim phenoSim;
    Annotations annotations;
    Classification classification;

    @Parameter(names={"--file", "-f"}, description="Path to VCF file", required=true)
    String file = "";

    @Parameter(names={"--phenotypes", "-p"}, description="List of phenotypes")
    List<String> phenotypes = new ArrayList<String>();

    @Parameter(names={"--omim", "-o"}, description="OMIM ID")
    String omim = "";

    @Parameter(names={"--imode", "-i"}, description="Mode of inheritance")
    String mode = "unknown";

    @Parameter(names={"--model", "-m"}, description="Prioritization model (Coding or Noncoding")
    String model = "Coding";


    public Main() throws Exception {
        this.props = this.getProperties();
    }

    public Properties getProperties() throws Exception {
        if (this.props != null) {
            return this.props;
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties props = new Properties();
        InputStream is = loader.getResourceAsStream(PROJECT_PROPERTIES);
        props.load(is);
        return props;
    }

    public void run() throws Exception {
        // this.runAnnotations(args);
        // this.runClassifications(args);
        // this.runPhenotypes(args);
        // this.sort();
        // this.merge();
        this.runTool();
    }

    public void runTool() throws Exception {
        this.phenoSim = new PhenoSim(this.props);
        this.annotations = new Annotations(this.props);
        this.classification = new Classification(this.props);
        log.info("Starting annotation");
        this.annotations.getAnnotations(this.file, this.mode);
    }

    public void runPhenotypes(String[] args) throws Exception {
        this.phenoSim = new PhenoSim(this.props);

        Set<String> phenotypes = new HashSet<String>();
        phenotypes.add("HP:0001084");
        phenotypes.add("HP:0003124");
        phenotypes.add("HP:0001114");

        Map<String, Double> sims = this.phenoSim.getGeneSimilarities(phenotypes);
        for (String gene: sims.keySet()) {
            System.out.println(gene + "\t" + sims.get(gene));
        }
        // Set<String> topPhenos = this.phenoSim.getTopLevelPhenotypes(phenotypes);
        // System.out.println(topPhenos.size());
        // for (String pheno: topPhenos) {
        //     System.out.println(pheno);
        // }

    }

    public void runClassifications(String[] args) throws Exception {
        this.classification = new Classification(this.props);

        if (args.length == 0) {
            throw new Exception("Please provide command name");
        }
        System.out.println("Running command " + args[0]);
        if (args[0].equals("toArffAll")) {
            this.classification.toArffAll();
        } else if (args[0].equals("classifyAll")) {
            this.classification.classifyAll();
        } else if (args[0].equals("sortAll")) {
            this.classification.sortAll();
        } else if (args[0].equals("classify")) {
            if (args.length != 2) {
                throw new Exception("Please provide arff file index");
            }
            int ind = Integer.parseInt(args[1]);
            this.classification.classify(ind);
        } else if (args[0].equals("classifyFiles")) {
            if (args.length != 2) {
                throw new Exception("Please provide arff file index");
            }
            int ind = Integer.parseInt(args[1]);
            this.classification.classifyFiles(ind);
        }
    }


    public void runAnnotations(String[] args) throws Exception {
        this.annotations = new Annotations(this.props);

        String root = args[0];
        File rootDir = new File(root);
        String[] files = rootDir.list(new FilenameFilter(){
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".vcf");
            }
        });
        // List<String> files = new ArrayList<String>();
        // try (BufferedReader br = Files.newBufferedReader(Paths.get("data/maxat.txt"))) {
        //     String line = null;
        //     while((line = br.readLine()) != null) {
        //         files.add(line + ".vcf");
        //     }
        // }

        int id = Integer.parseInt(args[1]);
        if (!root.endsWith("/")) {
            root = root + "/";
        }
        this.annotations.getAnnotations(root + files[id], "unknown");
    }

    public void sort() throws Exception {
        List<WGS> list = new ArrayList<WGS>();
        try(BufferedReader br = Files.newBufferedReader(Paths.get("data/db/wgs.txt"))) {
            String line;
            while((line = br.readLine()) != null) {
                String[] items = line.split("\t");
                int chr = 0;
                if (Character.isDigit(items[0].charAt(0))) {
                    chr = Integer.parseInt(items[0]);
                } else {
                    chr = (int)items[0].charAt(0);
                }
                int pos = Integer.parseInt(items[1]);
                list.add(new WGS(chr, pos, line));
            }
        }
        Collections.sort(list);
        PrintWriter out = new PrintWriter("data/db/db.txt");
        for (WGS wgs: list) {
            out.println(wgs.line);
        }
        out.close();
    }

    public void merge() throws Exception {
        PrintWriter out = new PrintWriter("data/db/wes.txt");
        try(BufferedReader br = Files.newBufferedReader(Paths.get("data/db/wes.vcf"))) {
            String line;
            while((line = br.readLine()) != null) {
                String[] items = line.split("\t");
                StringBuilder sb = new StringBuilder(items[0]);
                for (int i = 1; i < 4; i++) {
                    sb.append("\t");
                    sb.append(items[i]);
                }
                sb.append("\t");
                sb.append("Coding");
                for (int i = 4; i < items.length; i++) {
                    sb.append("\t");
                    sb.append(items[i]);
                }
                out.println(sb.toString());
            }
        }
        out.close();
    }

    class WGS implements Comparable<WGS> {
        String line;
        int chr;
        int pos;

        public WGS(int chr, int pos, String line) {
            this.chr = chr;
            this.pos = pos;
            this.line = line;
        }

        public int compareTo(WGS o) {
            int c = Integer.compare(this.chr, o.chr);
            if (c == 0)
                return Integer.compare(this.pos, o.pos);
            return c;
        }
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        new JCommander(main, args);
        main.run();
    }
}
