@Grab(group='com.github.sharispe', module='slib-sml', version='0.9.1')
@Grab(group='org.codehaus.gpars', module='gpars', version='1.1.0')

import java.net.*
import org.openrdf.model.vocabulary.*
import slib.sglib.io.loader.*
import slib.sml.sm.core.metrics.ic.utils.*
import slib.sml.sm.core.utils.*
import slib.sglib.io.loader.bio.obo.*
import org.openrdf.model.URI
import slib.graph.algo.extraction.rvf.instances.*
import slib.sglib.algo.graph.utils.*
import slib.utils.impl.Timer
import slib.graph.algo.extraction.utils.*
import slib.graph.model.graph.*
import slib.graph.model.repo.*
import slib.graph.model.impl.graph.memory.*
import slib.sml.sm.core.engine.*
import slib.graph.io.conf.*
import slib.graph.model.impl.graph.elements.*
import slib.graph.algo.extraction.rvf.instances.impl.*
import slib.graph.model.impl.repo.*
import slib.graph.io.util.*
import slib.graph.io.loader.*
import groovyx.gpars.GParsPool


def fout = new PrintWriter(new BufferedWriter(new FileWriter("phenosim-matrix.txt")))

def URI = "http://phenomebrowser.net/smltest/"
URIFactory factory = URIFactoryMemory.getSingleton()
URI graph_uri = factory.getURI(URI)
factory.loadNamespacePrefix("HP", graph_uri.toString())
G graph = new GraphMemory(graph_uri)

GDataConf graphconf = new GDataConf(GFormat.RDF_XML, "a-inferred.owl")
GraphLoaderGeneric.populate(graphconf, graph)

graph.removeE(RDF.TYPE)
URI virtualRoot = factory.getURI("http://phenomebrowser.net/smltest/virtualRoot");
graph.addV(virtualRoot);

// We root the graphs using the virtual root as root
GAction rooting = new GAction(GActionType.REROOTING);
rooting.addParameter("root_uri", virtualRoot.stringValue());
//GraphActionExecutor.applyAction(factory, rooting, graph);

//println graph.getE()

//load all entrrez to symbol mapping

def genemap = [:]

new File("ALL_SOURCES_ALL_FREQUENCIES_genes_to_phenotype.txt").splitEachLine("\t") { line ->
  if (!line[0].startsWith("#")) {
    def gid = "ENTREZ:"+line[0]
    def symbol = line[1]
    genemap[gid] = symbol
  }
}

new File("HMD_HumanPhenotype.rpt").splitEachLine("\t") { line ->
    def gid = "ENTREZ:"+line[1]?.trim()
    def symbol = line[0]?.trim()
    genemap[gid] = symbol
}

new File("ortho.txt").splitEachLine("\t") { line ->
  def gid = "ENTREZ:"+line[4]
  def symbol = line[3]
  genemap[gid] = symbol
}

def entrezset = new TreeSet()
new File("modelphenotypes.txt").splitEachLine("\t") { line ->
  def pat = /ENTREZ:\d+/
  def match = (line[2] =~ pat)
  match?.each { id ->
    if (id) {
      entrezset.add(id)
      def iduri = factory.getURI(URI+id)
      def pheno = line[1]
      def phenouri = null
      if (pheno.startsWith("HP:") || pheno.startsWith("MP:")) {
	phenouri = factory.getURI("http://purl.obolibrary.org/obo/"+(pheno.replaceAll(":","_")))
      } else if (pheno.length()>5) {
	phenouri = factory.getURI(pheno)
      }
      if (!phenouri || !graph.containsVertex(phenouri)) {
	//println "Missing $phenouri for $iduri"
      } else {
	//      println "Adding $iduri as new instance"
	Edge e = new Edge(iduri, RDF.TYPE, phenouri)
	graph.addE(e)
      }
    }
  }
}
GraphActionExecutor.applyAction(factory, rooting, graph);
println "Number of genes: "+entrezset.size()

ICconf icConf = new IC_Conf_Corpus("ResnikIC", SMConstants.FLAG_IC_ANNOT_RESNIK_1995_NORMALIZED)
SMconf smConfPairwise = new SMconf("Resnik", SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_RESNIK_1995 )
SMconf smConfGroupwise = new SMconf("BMA", SMConstants.FLAG_SIM_GROUPWISE_BMA)
smConfPairwise.setICconf(icConf)

InstancesAccessor ia = new InstanceAccessor_RDF_TYPE(graph)
SM_Engine engine = new SM_Engine(graph)

def id2gene = [:]
def map = [:].withDefault { new LinkedHashSet() }
new File("diseasephenotypes.txt").splitEachLine("\t") { line ->
  def id = line[0]
  if (id.indexOf("OMIM")>-1) {
    def pheno = line[1]
    if (pheno.startsWith("HP") || pheno.startsWith("MP")) {
      pheno = pheno.replaceAll("HP:","http://purl.obolibrary.org/obo/HP_")
      pheno = pheno.replaceAll("MP:","http://purl.obolibrary.org/obo/MP_")
    }
    if (pheno.startsWith("http")) {
      def p = factory.getURI(pheno)
      if (graph.containsVertex(p)) {
	map[id].add(p)
      }
    }
  }
}

GParsPool.withPool {
  map.eachParallel { id, phenoset ->
    println "Model: "+id
    engine.getInstances().each { dis ->
      def set2 = ia.getDirectClass(dis)
      if (set2) {
		def dis2 = dis.toString().replaceAll("http://phenomebrowser.net/smltest/","")
		fout.println("$id\t$dis2\t"+genemap[dis2]+"\t"+engine.compare(smConfGroupwise, smConfPairwise, phenoset, set2))
      }
    }
  }
}

fout.flush()
fout.close()
