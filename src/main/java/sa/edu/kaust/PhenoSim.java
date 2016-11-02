package sa.edu.kaust;

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.net.*;
import org.openrdf.model.vocabulary.*;
import slib.sml.sm.core.metrics.ic.utils.*;
import slib.sml.sm.core.utils.*;
import org.openrdf.model.URI;
import slib.graph.algo.extraction.rvf.instances.*;
import slib.utils.impl.Timer;
import slib.graph.algo.extraction.utils.*;
import slib.graph.model.graph.*;
import slib.graph.model.repo.*;
import slib.graph.model.impl.graph.memory.*;
import slib.sml.sm.core.engine.*;
import slib.graph.io.conf.*;
import slib.graph.model.impl.graph.elements.*;
import slib.graph.algo.extraction.rvf.instances.impl.*;
import slib.graph.model.impl.repo.*;
import slib.graph.io.util.*;
import slib.graph.io.loader.*;


public class PhenoSim {
    public static final String PHENO_URI = "http://purl.obolibrary.org/obo/";
    public static final String GRAPH_URI = "http://phenomebrowser.net/smltest/";
    URIFactory uriFactory;
    G graph;
    SM_Engine engine;
    InstancesAccessor instanceAccessor;
    SMconf smConfPairwise;
    SMconf smConfGroupwise;
    boolean h;
    boolean mod;
    Properties props;
    Map<String, Set<String> > topPhenos;


    public PhenoSim(Properties props, boolean human, boolean model) throws Exception {
        this.props = props;
        this.h = human;
        this.mod = model;
        System.setProperty("jdk.xml.entityExpansionLimit", "0");
        System.setProperty("jdk.xml.totalEntitySizeLimit", "0");
        this.initEngine();
        this.loadTopLevelPhenotypes();
    }

    private void initEngine() throws Exception {

        String uri = GRAPH_URI;
        this.uriFactory = URIFactoryMemory.getSingleton();
        URI graphURI = this.uriFactory.getURI(uri);
        this.uriFactory.loadNamespacePrefix("HP", graphURI.toString());
        this.graph = new GraphMemory(graphURI);

        String aOntology = this.props.getProperty("aOntology");
        GDataConf graphconf = new GDataConf(
            GFormat.RDF_XML, aOntology);
        GraphLoaderGeneric.populate(graphconf, this.graph);

        this.graph.removeE(RDF.TYPE);
        URI virtualRoot = this.uriFactory.getURI(
            uri + "virtualRoot");
        this.graph.addV(virtualRoot);

        // We root the graphs using the virtual root as root
        GAction rooting = new GAction(GActionType.REROOTING);
        rooting.addParameter("root_uri", virtualRoot.stringValue());
        //GraphActionExecutor.applyAction(factory, rooting, graph);

        //println graph.getE()
        String modelPhenoFile;
        if (this.h)
            modelPhenoFile = this.props.getProperty("modelPhenoFile_human");
        else if (this.mod)
            modelPhenoFile = this.props.getProperty("modelPhenoFile_mod");
        else
            modelPhenoFile = this.props.getProperty("modelPhenoFile");
        try(BufferedReader br = Files.newBufferedReader(Paths.get(modelPhenoFile))) {
            String line;
            while((line = br.readLine()) != null) {
                String[] items = line.split("\t");
                String geneIds[] = items[2].substring(1, items[2].length() - 1).split(", ");
                String id = geneIds[0];
                for (int i = 1; i < geneIds.length; i++) {
                  id += "_" + geneIds[i];
              }
              id = URLEncoder.encode(id);
              URI idURI = uriFactory.getURI(uri + id);
              String pheno = items[1].replaceAll(":", "_");
              URI phenoURI = uriFactory.getURI("http://purl.obolibrary.org/obo/" + pheno);
              Edge e = new Edge(idURI, RDF.TYPE, phenoURI);
              this.graph.addE(e);
            }
        }

        GraphActionExecutor.applyAction(uriFactory, rooting, this.graph);

        ICconf icConf = new IC_Conf_Corpus("ResnikIC", SMConstants.FLAG_IC_ANNOT_RESNIK_1995_NORMALIZED);
        this.smConfPairwise = new SMconf("Resnik", SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_RESNIK_1995 );
        this.smConfGroupwise = new SMconf("BMA", SMConstants.FLAG_SIM_GROUPWISE_BMA);
        smConfPairwise.setICconf(icConf);

        this.instanceAccessor = new InstanceAccessor_RDF_TYPE(this.graph);
        this.engine = new SM_Engine(this.graph);
    }

    private void loadTopLevelPhenotypes() throws Exception {
        String filename = this.props.getProperty("topLevelPhenoFile");
        this.topPhenos = new HashMap<String, Set<String> >();
        try(BufferedReader br = Files.newBufferedReader(Paths.get(filename))) {
            String line;
            while((line = br.readLine()) != null) {
                if (line.equals("")) {
                    continue;
                }
                String[] items = line.split("\t", -1);
                String phenotype = items[0];
                if (!this.topPhenos.containsKey(phenotype)) {
                    this.topPhenos.put(phenotype, new HashSet<String>());
                }
                Set<String> phenoSet = this.topPhenos.get(phenotype);
                String[] topPheno1 = items[1].split(", ");
                for (String pheno: topPheno1) {
                    phenoSet.add(pheno);
                }
                String[] topPheno2 = items[2].split(", ");
                for (String pheno: topPheno2) {
                    phenoSet.add(pheno);
                }
            }
        }
    }

    public Map<String, Double> getGeneSimilarities(Set<String> phenotypes) throws Exception {
        Set<URI> phenoURIs = new HashSet<URI>();
        for (String pheno: phenotypes) {
            pheno = pheno.replaceAll("HP:", "HP_");
            pheno = pheno.replaceAll("MP:", "MP_");
            URI phenoURI = this.uriFactory.getURI(PHENO_URI + pheno);
            if (this.graph.containsVertex(phenoURI)) {
                phenoURIs.add(phenoURI);
            }
        }
        Map<String, Double> result = new HashMap<String, Double>();
        double maxScore = Double.MIN_VALUE;
        double minScore = Double.MAX_VALUE;
        for (URI gene: this.engine.getInstances()) {
            Set<URI> set = this.instanceAccessor.getDirectClass(gene);
            if (set != null && set.size() > 0) {
                String geneIds = URLDecoder.decode(gene.toString());
                geneIds = geneIds.substring(34, geneIds.length());
                Double score = 0.0;
                if (phenoURIs.size() > 0) {
                    score = engine.compare(
                        smConfGroupwise, smConfPairwise, phenoURIs, set);
                }
                maxScore = Math.max(maxScore, score);
                minScore = Math.min(minScore, score);
                String[] geneId = geneIds.split("_");
                for (String gId: geneId) {
                    if (!result.containsKey(gId)) {
                        result.put(gId, score);
                    } else {
                        Double oldScore = result.get(gId);
                        result.put(gId, Math.max(score, oldScore));
                    }
                }
            }
        }
        // Min-Max Normalization
        for (String gene: result.keySet()) {
            double score = result.get(gene);
            result.put(gene, (score - minScore) / (maxScore - minScore));
        }
        return result;
    }

    public Set<String> getTopLevelPhenotypes(Set<String> phenotypes) {
        Set<String> result = new HashSet<String>();
        for (String pheno: phenotypes) {
            if (this.topPhenos.containsKey(pheno)) {
                result.addAll(this.topPhenos.get(pheno));
            }
        }
        return result;
    }
}
