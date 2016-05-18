import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.file.*;
import htsjdk.tribble.readers.TabixReader;

public class Main {

    public static final String PROJECT_PROPERTIES = "project.properties";
    Properties props;
    PhenoSim phenoSim;
    Annotations annotations;
    Classification classification;

    public Main() throws Exception {
        this.props = this.getProperties();
        // this.phenoSim = new PhenoSim(this.props);
        // this.annotations = new Annotations(this.props);
        this.classification = new Classification(this.props);
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

    public void run(String[] args) throws Exception {
        // Set<String> phenotypes = new HashSet<String>();
        // phenotypes.add("HP:0000006");
        // phenotypes.add("HP:0002664");
        // phenotypes.add("HP:0012125");
        // Map<String, Double> sims = this.phenoSim.getGeneSimilarities(phenotypes);
        // Set<String> topPhenos = this.phenoSim.getTopLevelPhenotypes(phenotypes);
        // System.out.println(sims.size());
        // System.out.println(topPhenos.size());
        // for (String pheno: topPhenos) {
        //     System.out.println(pheno);
        // }
        // this.annotations.getAnnotations("data/adeeb.vcf");
        // this.classification.classify();
        // this.annotations.readGzip();
        // System.out.println(Arrays.toString(args));
        // this.classification.toArffAll();
        // this.classification.classifyAll();
        // this.classification.sortAll();
        // this.annotations.readDbFile();
        // this.runAnnotations(args);
        this.runCommand(args);
    }

    public void runCommand(String[] args) throws Exception {
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
            if (args.length != 3) {
                throw new Exception("Please provide arff file index");
            }
            int ind = Integer.parseInt(args[1]);
            this.classification.classifyFiles(ind);
        }
    }


    public void runAnnotations(String[] args) throws Exception {
        String root = args[0];
        // File rootDir = new File(root);
        // String[] files = rootDir.list(new FilenameFilter(){
        //     @Override
        //     public boolean accept(File dir, String name) {
        //         return name.toLowerCase().endsWith(".vcf");
        //     }
        // });
        List<String> files = new ArrayList<String>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get("data/maxat.txt"))) {
            String line = null;
            while((line = br.readLine()) != null) {
                files.add(line + ".vcf");
            }
        }

        int id = Integer.parseInt(args[1]);
        if (!root.endsWith("/")) {
            root = root + "/";
        }
        this.annotations.getAnnotations(root + files.get(id));
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
