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
import org.json.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.alg.*;


public class Main {

    public static final String PROJECT_PROPERTIES = "project.properties";
    Logger log = Logger.getLogger(Main.class.getName());
    Properties props;
    PhenoSim phenoSim;
    Annotations annotations;
    Classification classification;
	String mode = "3";
    Map<String, String> inhModes;
    Map<String, List<String> > disPhenos;
	Map<String, List<String> > interactions;
	UndirectedGraph<String, DefaultEdge> actions;
	
    @Parameter(names={"--file", "-f"}, description="Path to VCF file", required=true)
    String inFile = "";
    @Parameter(names={"--outfile", "-of"}, description="Path to results file", required=false)
    String outFile = "";

    @Parameter(names={"--phenotypes", "-p"}, description="List of phenotype ids separated by commas")
    String phenos = "";

    List<String> phenotypes = new ArrayList<String>();

    @Parameter(names={"--omim", "-o"}, description="OMIM ID")
    String omimId = "";

    @Parameter(names={"--inh", "-i"}, description="Mode of inheritance")
    String inh = "unknown";

    String model = "Coding";

    @Parameter(names={"--human", "-h"}, description="Propagate human disease phenotypes to genes only")
    boolean human = false;

    @Parameter(names={"--sp", "-s"}, description="Propagate mouse and fish disease phenotypes to genes only")
    boolean mod = false;
	
	@Parameter(names={"--json", "-j"}, description="Path to PhenoTips JSON file containing phenotypes")
    String jsonFile = "";
	
	@Parameter(names={"--digenic", "-d"}, description="Rank digenic combinations")
    boolean digenic = false;
	
	@Parameter(names={"--trigenic", "-t"}, description="Rank trigenic combinations")
    boolean trigenic = false;
	
	@Parameter(names={"--combination", "-c"}, description="Maximum Number of variant combinations to prioritize (for digenic and trigenic cases only)")
    int c = 1000;

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

	private void loadInteractions() throws Exception {
        String filename = this.props.getProperty("interFile");
        this.interactions = new HashMap<String, List<String> >();
		//this.actions = new SimpleGraph<String, DefaultEdge>(DefaultEdge.class);
        try(BufferedReader br = Files.newBufferedReader(Paths.get(filename))) {
            String line;
            while((line = br.readLine()) != null) {
                if (line.equals("")) {
                    continue;
                }
                String[] items = line.split("\t", -1);
                String inter = items[0];
                if (!this.interactions.containsKey(inter)) {
                    this.interactions.put(inter, new ArrayList<String>());
                }
                List<String> actionSet = this.interactions.get(inter);
                String action = items[1];
                actionSet.add(action);
            }
        }
		//add data into graph
		/*for (String v:this.interactions.keySet()) {
			this.actions.addVertex(v);
		}
		for (String v:this.interactions.keySet()) {
			for (String w:this.interactions.get(v))
				this.actions.addEdge(v, w);
		}*/
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
        this.runTool();
    }

    public void validateParameters() throws Exception {
        if (!this.phenos.equals("")) {
            for (String pheno: this.phenos.split(",")) {
                pheno = pheno.trim();
                if (!(pheno.matches("HP:\\d{7}") || pheno.matches("MP:\\d{7}"))) {
                    throw new PhenotypeFormatException("Wrong phenotype format. Should be HP:XXXXXXX or MP:XXXXXXX");
                }
                this.phenotypes.add(pheno);
				this.mode = "0"; //run as dominant mode
            }
        } else if (!this.omimId.equals("")){
            if (!this.omimId.matches("OMIM:\\d{6}")) {
                throw new Exception("Wrong OMIM ID format. Should be OMIM:XXXXXX");
            }
            if (this.disPhenos.containsKey(this.omimId)) {
                this.phenotypes = this.disPhenos.get(this.omimId);
            } else {
                // throw new Exception("OMIM ID not found in our database");
            }
            if (this.inh.equals("unknown") && this.inhModes.containsKey(this.omimId)) {
                this.inh = this.inhModes.get(this.omimId);
            }
		} else if (!this.jsonFile.equals("")){
			//Load phenotypes from JSON file
			String content = new String(Files.readAllBytes(Paths.get(jsonFile)));
			JSONObject obj = new JSONObject(content.trim());
			JSONArray arr = obj.getJSONArray("features");
			for (int i = 0; i < arr.length(); i++){
				String id = arr.getJSONObject(i).getString("id");
				if (id.matches("HP:\\d{7}")) {
					this.phenotypes.add(id);
				}
			}
        } else {
            // throw new Exception("Please provide phenotypes or OMIM ID");
        }

        if (this.outFile.equals("")) {
            this.outFile = this.inFile;
        }

    }

    public void runTool() {
        try {
            log.info("Initializing the model");
            this.loadInhModes();
            this.loadDiseasePhenotypes();
			this.loadInteractions();
            this.validateParameters();
            for (String pheno: this.phenotypes) {
                System.out.println(pheno);
            }
            this.phenoSim = new PhenoSim(this.props, this.human, this.mod);
            this.annotations = new Annotations(this.props);
            this.classification = new Classification(this.props);
            log.info("Computing similarities");
            Set<String> phenotypes = new HashSet<String>(this.phenotypes);
            Map<String, Double> sims = this.phenoSim.getGeneSimilarities(phenotypes);
            log.info("Getting top level phenotypes");
            Set<String> topPhenos = this.phenoSim.getTopLevelPhenotypes(phenotypes);
			log.info("Starting annotation");
			this.inh = this.inh.toLowerCase();
			if (this.inh.equals("dominant")) {
				this.mode = "0";
			} else if (this.inh.equals("recessive")) {
				this.mode = "1";
			} else if (this.inh.equals("x-linked")) {
				this.mode = "2";
			}
            this.annotations.getAnnotations(
                this.inFile, this.outFile, this.inh, this.model, sims);
			this.classification.toCSV(this.outFile, topPhenos, this.mode);
            this.classification.toolClassify(this.outFile);
			if (digenic) {
				List<String> genes = new ArrayList<String>();
				genes.addAll(this.interactions.keySet());
				this.classification.toolFilter(this.outFile, genes, 2);
				this.classification.toolDigenic(this.outFile, this.interactions, 2, c);
			}
			if (trigenic) {
				List<String> genes = new ArrayList<String>();
				genes.addAll(this.interactions.keySet());
				this.classification.toolFilter(this.outFile, genes, 3);
				this.classification.toolTrigenicOpt(this.outFile, this.interactions, 3, c);
			}
        } catch(PhenotypeFormatException e) {
            log.severe(e.getMessage());
        } catch(Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            log.severe(sw.toString());
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
