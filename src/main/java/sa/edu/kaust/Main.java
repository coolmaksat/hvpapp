package sa.edu.kaust;

import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.file.*;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import java.util.logging.Level;
import java.util.logging.Logger;
import sa.edu.kaust.exceptions.*;


public class Main {

    public static final String PROJECT_PROPERTIES = "project.properties";
    Logger log = Logger.getLogger(Main.class.getName());
    Properties props;
    PhenoSim phenoSim;
    Annotations annotations;
    Classification classification;
    Map<String, String> inhModes;
    Map<String, List<String> > disPhenos;

    @Parameter(names={"--file", "-f"}, description="Path to VCF file", required=true)
    String file = "";

    @Parameter(names={"--phenotypes", "-p"}, description="List of phenotype ids separated by commas")
    String phenos = "";

    List<String> phenotypes = new ArrayList<String>();

    @Parameter(names={"--omim", "-o"}, description="OMIM ID")
    String omimId = "";

    @Parameter(names={"--inh", "-i"}, description="Mode of inheritance")
    String inh = "unknown";

    @Parameter(names={"--model", "-m"}, description="Prioritization model (Coding or Noncoding)")
    String model = "Coding";


    public Main() throws Exception {
        this.props = this.getProperties();
    }

    private void loadInhModes() throws Exception {
        String fileName = this.props.getProperty("inhModesFile");
        this.inhModes = new HashMap<String, String>();
        try(BufferedReader br = Files.newBufferedReader(Paths.get(fileName))) {
            String line;
            while((line = br.readLine()) != null) {
                if (line.equals("")) {
                    continue;
                }
                String[] items = line.split("\t", -1);
                String dis = items[0];
                String inh = items[1];
                this.inhModes.put(dis, inh);
            }
        }
    }

    private void loadDiseasePhenotypes() throws Exception {
        String filename = this.props.getProperty("disPhenoFile");
        this.disPhenos = new HashMap<String, List<String> >();
        try(BufferedReader br = Files.newBufferedReader(Paths.get(filename))) {
            String line;
            while((line = br.readLine()) != null) {
                if (line.equals("")) {
                    continue;
                }
                String[] items = line.split("\t", -1);
                String dis = items[0];
                if (!this.disPhenos.containsKey(dis)) {
                    this.disPhenos.put(dis, new ArrayList<String>());
                }
                List<String> phenoSet = this.disPhenos.get(dis);
                String pheno = items[1];
                phenoSet.add(pheno);
            }
        }
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
        // this.runPhenotypes();
        // this.sort();
        // this.merge();
        this.runTool();
    }

    public void validateParameters() throws Exception {
        if (!(this.model.equals("Coding") || this.model.equals("Noncoding"))) {
            throw new Exception("Model should be Coding or Noncoding");
        }
        if (!this.phenos.equals("")) {
            for (String pheno: this.phenos.split(",")) {
                pheno = pheno.trim();
                if (!(pheno.matches("HP:\\d{7}") || pheno.matches("MP:\\d{7}"))) {
                    throw new PhenotypeFormatException("Wrong phenotype format. Should be HP:XXXXXXX or MP:XXXXXXX");
                }
                this.phenotypes.add(pheno);
            }
        } else if (!this.omimId.equals("")){
            if (!this.omimId.matches("OMIM:\\d{6}")) {
                throw new Exception("Wrong OMIM ID format. Should be OMIM:XXXXXX");
            }
            if (this.disPhenos.containsKey(this.omimId)) {
                this.phenotypes = this.disPhenos.get(this.omimId);
            } else {
                throw new Exception("OMIM ID not found in our database");
            }
            if (this.inh.equals("unknown") && this.inhModes.containsKey(this.omimId)) {
                this.inh = this.inhModes.get(this.omimId);
            }
        } else {
            throw new Exception("Please provide phenotypes or OMIM ID");
        }

    }

    public void runTool() {
        try {
            this.validateParameters();
            log.info("Initializing the model");
            this.loadInhModes();
            this.loadDiseasePhenotypes();
            this.phenoSim = new PhenoSim(this.props);
            this.annotations = new Annotations(this.props);
            this.classification = new Classification(this.props, this.model);

            log.info("Computing similarities");
            Set<String> phenotypes = new HashSet<String>(this.phenotypes);
            Map<String, Double> sims = this.phenoSim.getGeneSimilarities(phenotypes);
            log.info("Getting top level phenotypes");
            Set<String> topPhenos = this.phenoSim.getTopLevelPhenotypes(phenotypes);
            log.info("Starting annotation");
            this.inh = this.inh.toLowerCase();
            String mode = "3";
            if (this.inh.equals("dominant")) {
                mode = "0";
            } else if (this.inh.equals("recessive")) {
                mode = "1";
            } else if (this.inh.equals("x-linked")) {
                mode = "2";
            }
            this.annotations.getAnnotations(
                this.file, this.inh, this.model, sims);
            this.classification.toArff(this.file, topPhenos, mode);
            this.classification.toolClassify(this.file);
        } catch(PhenotypeFormatException e) {
            log.severe(e.getMessage());
        } catch(Exception e) {
            log.severe(e.getMessage());
        }
    }

    public void runPhenotypes() throws Exception {
        this.phenoSim = new PhenoSim(this.props);

        Set<String> phenotypes = new HashSet<String>();
        phenotypes.add("HP:0010662");
        phenotypes.add("HP:0002930");
        phenotypes.add("HP:0008227");
        phenotypes.add("HP:0008247");
        phenotypes.add("HP:0000836");
        phenotypes.add("HP:0000818");
        phenotypes.add("HP:0000853");
        phenotypes.add("HP:0003828");
        phenotypes.add("HP:0001962");
        phenotypes.add("HP:0001649");
        phenotypes.add("HP:0011784");
        phenotypes.add("HP:0003812");
        phenotypes.add("MP:0001255");
        phenotypes.add("MP:0001253");
        phenotypes.add("MP:0005605");
        phenotypes.add("MP:0005422");
        phenotypes.add("HP:0002750");
        phenotypes.add("HP:0200000");
        phenotypes.add("HP:0010514");
        phenotypes.add("HP:0011344");
        phenotypes.add("HP:0001263");
        phenotypes.add("HP:0002342");
        Map<String, Double> sims = this.phenoSim.getGeneSimilarities(phenotypes);
        for (String gene: sims.keySet()) {
            System.out.println(gene + "\t" + sims.get(gene));
        }
        Set<String> topPhenos = this.phenoSim.getTopLevelPhenotypes(phenotypes);
        System.out.println(topPhenos.size());
        for (String pheno: topPhenos) {
            System.out.println(pheno);
        }

    }

    public void runClassifications(String[] args) throws Exception {
        this.classification = new Classification(this.props, this.model);

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
        // this.annotations.getAnnotations(root + files[id], this.mode, this.model);
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

    public static void main(String[] args) {
        JCommander jCommander = null;
        Main main = null;
        try {
            main = new Main();
            jCommander = new JCommander(main);
            jCommander.setProgramName("HVPapp");
            jCommander.parse(args);
            main.run();
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            jCommander.usage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
