import java.io.*;
import java.util.*;
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
        this.annotations = new Annotations(this.props);
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
        this.annotations.getAnnotations("data/adeeb.vcf");
        // this.classification.classify();
        // this.annotations.readGzip();
        // System.out.println(Arrays.toString(args));
        // this.classification.toArffAll();
        // this.classification.classifyAll();
        // this.classification.sortAll();
        // this.annotations.readDbFile();
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
