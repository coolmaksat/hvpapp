package sa.edu.kaust;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.nio.*;
import java.nio.file.*;
import org.jgrapht.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.alg.*;

public class Classification {

    public String modelFile = "";
    public String[] topLevelPhenotypes;
	public Map<String, List<String> > actions;
	List<String> genes;
	public Map<String, List<String> > geneVars;


    public Classification(Properties props) throws Exception {
        // Loading the saved classifier
        this.modelFile = props.getProperty("model");
        this.topLevelPhenotypes = props.getProperty("topLevelPhenotypes").split(", ");
    }


    public void toolClassify(String fileName) throws Exception {
		long fileCount = Files.lines(Paths.get(fileName + ".csv")).count() - 1;
		Double[] results = new Double[(int)fileCount];
        Classification that = this;
		String param1 = fileName + ".csv";
		String param2 = this.modelFile;
		String line = null;
		String pythonScript;
		pythonScript = "data/score.py";
		try {
		ProcessBuilder pb = new ProcessBuilder("python",pythonScript,""+param1,""+param2);
		Process p = pb.start();
		int x = 0;
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		while ((line = in.readLine()) != null) {
			results[x] = Double.valueOf(line);
			x++;
		}
		int val = p.waitFor();
		p.getInputStream().close();
		p.getOutputStream().close();
		p.getErrorStream().close();
		if (val == 0) {
			System.out.println("Finished Annotation");
			DataResult[] data = new DataResult[results.length];
			try(BufferedReader br = Files.newBufferedReader(Paths.get(fileName + ".out"))) {
				line = null;
				int i = 0;
				while((line = br.readLine()) != null) {
					data[i] = new DataResult(line, results[i]);
					++i;
				}
				br.close();
			}
			Arrays.sort(data, Collections.reverseOrder());

			PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter(fileName + ".res"), 104857600));
			out.println("Chr\tStart\tRef\tAlt\tGT\tGene\tCADD\tGWAVA\tDANN\tSim_Score\tPrediction_Score");
			for (int i = 0; i < data.length; i++) {
				out.println(data[i].s + "\t" + data[i].r);
			}
			out.flush();
			out.close();
		}
		else
			System.out.println("Error in annotation!");
		} catch (Exception e) {
			e.printStackTrace();
    }
    }

	public void toolFilter(String fileName, List<String> genes, int param) throws Exception {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName + ".top"), 1073741824));
		List<String> vars = new ArrayList<String>();
		try(BufferedReader br = Files.newBufferedReader(Paths.get(fileName + ".res"))) {
            String line;
			List<String> myList= new ArrayList<String>();
			Map<String, Integer> geneCount = new HashMap<String, Integer>();
            while((line = br.readLine()) != null) {
                String[] items = line.split("\t", -1);
				String mygene = items[5];
				//check if not in my list, add it, and write line
				if (genes.contains(mygene)) {
					if (!myList.contains(mygene)) {
						out.println(line);
						vars.add(line);
						myList.add(mygene);
						geneCount.put(mygene, 1);
					}
					else {
						//check if count doesn't exceed param
						int count = geneCount.get(mygene);
						if (count < param) {
							geneCount.put(mygene, count + 1);
							out.println(line);
							vars.add(line);
						}
					}
				}
			}
			br.close();
        }
        out.flush();
        out.close();
}

	public void toolDigenic(String fileName, Map<String, List<String> > interactions, int param, int topCount) throws Exception {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName + ".digenic"), 1073741824));
		for (int i = 1; i<=param; i++) {
		out.print("Chr"+i+"\tStart"+i+"\tRef"+i+"\tAlt"+i+"\tGT"+i+"\tGene"+i+"\tCADD\tSim_Score\tPrediction_Score"+i+"\t");
		}
		out.println("Combined_Score");
		try(BufferedReader br = Files.newBufferedReader(Paths.get(fileName + ".top"))) {
            String line;
			List<String> vars = new ArrayList<String>();
            while((line = br.readLine()) != null) {
                 vars.add(line);
            }
			br.close();
		List<DataResult> data = new ArrayList<DataResult>();
		List<Double> dataScores = new ArrayList<Double>();
		//process combinations
		for(int i=0; i < vars.size(); i++){
			double combined_score = 0.0;
			String[] set1 = vars.get(i).split("\t", -1);
			String first_var = "";
			String s = "";
			for (int x=0; x < 7; x++){
				first_var += set1[x] + "\t";
			}
			first_var += set1[9] + "\t";
			first_var += set1[10];
			String first_gene = vars.get(i).split("\t", -1)[5];
			double first_pred = Double.parseDouble(vars.get(i).split("\t", -1)[10]);
			//second loop
			for (int j=i+1; j < vars.size(); j++) {
				String[] set2 = vars.get(j).split("\t", -1);
				String second_var = "";
				for (int y=0; y < 7; y++){
					second_var += set2[y] + "\t";
				}
				second_var += set2[9] + "\t";
				second_var += set2[10];
				String second_gene = vars.get(j).split("\t", -1)[5];
				double second_pred = Double.parseDouble(vars.get(j).split("\t", -1)[10]);
				//check if both genes interact
				if ((interactions.get(first_gene).contains(second_gene))){
					combined_score = first_pred + second_pred;
					s = first_var + "\t" + second_var;
					if (data.size() < topCount) { 
						DataResult mydata = new DataResult(s, combined_score);
						data.add(mydata);
						dataScores.add(combined_score);
					}
					else { 
						double minVal = Collections.min(dataScores);
						if ( minVal < combined_score ) {
							int minIndex = dataScores.indexOf(minVal);
							DataResult mydata = new DataResult(s, combined_score);
							data.set(minIndex, mydata);
							dataScores.set(minIndex,combined_score); 
						}
					}
				}
			}
		}
		Collections.sort(data, Collections.reverseOrder());
		if (topCount < data.size()) {
			for (int i = 0; i < topCount; i++) {
				out.println(data.get(i).s + "\t" + data.get(i).r);
			}
		}
		else {
			for (int i = 0; i < data.size(); i++) {
				out.println(data.get(i).s + "\t" + data.get(i).r);
			}
		}
        out.flush();
        out.close();
		}
    }

	public void toolTrigenic(String fileName, Map<String, List<String> > interactions, int param, int topCount) throws Exception {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName + ".trigenic"), 1073741824));
		for (int i = 1; i<=param; i++) {
		out.print("Chr"+i+"\tStart"+i+"\tRef"+i+"\tAlt"+i+"\tGT"+i+"\tGene"+i+"\tCADD\tSim_Score\tPrediction_Score"+i+"\t");
		}
		out.println("Combined_Score");
		try(BufferedReader br = Files.newBufferedReader(Paths.get(fileName + ".top"))) {
            String line;
			List<String> vars = new ArrayList<String>();
            while((line = br.readLine()) != null) {
                 vars.add(line);
            }
			br.close();
		//process combinations
		List<DataResult> data = new ArrayList<DataResult>();
		List<Double> dataScores = new ArrayList<Double>();
		for(int i=0; i < vars.size(); i++){
			//System.out.println(i);
			double combined_score = 0.0;
			String[] set1 = vars.get(i).split("\t", -1);
			String first_var = "";
			String s = "";
			for (int x=0; x < 7; x++){
				first_var += set1[x] + "\t";
			}
			first_var += set1[9] + "\t";
			first_var += set1[10];
			String first_gene = set1[5];
			double first_pred = Double.parseDouble(set1[10]);
			//second loop
			for (int j=i+1; j < vars.size(); j++) {
				String[] set2 = vars.get(j).split("\t", -1);
				String second_var = "";
				for (int y=0; y < 7; y++){
					second_var += set2[y] + "\t";
				}
				second_var += set2[9] + "\t";
				second_var += set2[10];
				String second_gene = set2[5];
				double second_pred = Double.parseDouble(set2[10]);
				//third loop
				for (int k=j+1; k < vars.size(); k++) {
					String[] set3 = vars.get(k).split("\t", -1);
					String third_var = "";
						for (int z=0; z < 7; z++){
						third_var += set3[z] + "\t";
						}
					third_var += set3[9] + "\t";
					third_var += set3[10];
					String third_gene = set3[5];
					double third_pred = Double.parseDouble(set3[10]);
					//if genes are identical
					if ((first_gene.equals(second_gene)) && (second_gene.equals(third_gene))) {
						combined_score = first_pred + second_pred + third_pred;
						s = first_var + "\t" + second_var + "\t" + third_var;
						if (data.size() < topCount) { 
							DataResult mydata = new DataResult(s, combined_score);
							data.add(mydata);
							dataScores.add(combined_score);
						}
						else { 
							double minVal = Collections.min(dataScores);
							if ( minVal < combined_score ) {
								int minIndex = dataScores.indexOf(minVal);
								DataResult mydata = new DataResult(s, combined_score);
								data.set(minIndex, mydata);
								dataScores.set(minIndex,combined_score); 
							}
						}
					}
					else {
						if ((interactions.get(first_gene).contains(second_gene)) && (interactions.get(first_gene).contains(third_gene))){
							combined_score = first_pred + second_pred + third_pred;
							s = first_var + "\t" + second_var + "\t" + third_var;
							if (data.size() < topCount) { 
								DataResult mydata = new DataResult(s, combined_score);
								data.add(mydata);
								dataScores.add(combined_score);
							}
							else { 
								double minVal = Collections.min(dataScores);
								if ( minVal < combined_score ) {
									int minIndex = dataScores.indexOf(minVal);
									DataResult mydata = new DataResult(s, combined_score);
									data.set(minIndex, mydata);
									dataScores.set(minIndex,combined_score); 
								}
							}
						}
						else if ((interactions.get(first_gene).contains(second_gene)) && (interactions.get(second_gene).contains(third_gene))){
							combined_score = first_pred + second_pred + third_pred;
							s = first_var + "\t" + second_var + "\t"+ third_var;
							if (data.size() < topCount) { 
								DataResult mydata = new DataResult(s, combined_score);
								data.add(mydata);
								dataScores.add(combined_score);
							}
							else { 
								double minVal = Collections.min(dataScores);
								if ( minVal < combined_score ) {
									int minIndex = dataScores.indexOf(minVal);
									DataResult mydata = new DataResult(s, combined_score);
									data.set(minIndex, mydata);
									dataScores.set(minIndex,combined_score); 
								}
							}
						}
						else if ((interactions.get(first_gene).contains(third_gene)) && (interactions.get(third_gene).contains(second_gene))){
							combined_score = first_pred + second_pred + third_pred;
							s = first_var + "\t" + second_var + "\t"+ third_var;
							if (data.size() < topCount) { 
								DataResult mydata = new DataResult(s, combined_score);
								data.add(mydata);
								dataScores.add(combined_score);
							}
							else { 
								double minVal = Collections.min(dataScores);
								if ( minVal < combined_score ) {
									int minIndex = dataScores.indexOf(minVal);
									DataResult mydata = new DataResult(s, combined_score);
									data.set(minIndex, mydata);
									dataScores.set(minIndex,combined_score); 
								}
							}
						}
					}
				}
			}
		}
		Collections.sort(data, Collections.reverseOrder());
		if (topCount < data.size()) {
			for (int i = 0; i < topCount; i++) {
				out.println(data.get(i).s + "\t" + data.get(i).r);
			}
		}
		else {
			for (int i = 0; i < data.size(); i++) {
				out.println(data.get(i).s + "\t" + data.get(i).r);
			}
		}
        out.flush();
        out.close();
		}
    }
	public void toolTrigenicOpt(String fileName, Map<String, List<String> > interactions, int param, int topCount) throws Exception {
		this.actions = new HashMap<String, List<String> >();
		this.genes = new ArrayList<String>();
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName + ".trigenic"), 1073741824));
		for (int i = 1; i<=param; i++) {
		out.print("Chr"+i+"\tStart"+i+"\tRef"+i+"\tAlt"+i+"\tGT"+i+"\tGene"+i+"\tCADD\tSim_Score\tPrediction_Score"+i+"\t");
		}
		out.println("Combined_Score");
		this.geneVars = new HashMap<String, List<String> >();
		try(BufferedReader br = Files.newBufferedReader(Paths.get(fileName + ".top"))) {
            String line;
			List<String> vars = new ArrayList<String>();
            while((line = br.readLine()) != null) {
                vars.add(line);
				String[] items = line.split("\t", -1);
				String gene = items[5];
				if (!this.geneVars.containsKey(gene)) {
					this.geneVars.put(gene, new ArrayList<String>());
				}
				List<String> varSet = this.geneVars.get(gene);
				varSet.add(line);
			}
			br.close();
		//process combinations
		List<DataResult> data = new ArrayList<DataResult>();
		List<Double> dataScores = new ArrayList<Double>();
		List<String> combo = new ArrayList<String>();
		this.genes.addAll(geneVars.keySet());
		for (String g: this.genes) {
			if (!this.actions.containsKey(g)) {
				this.actions.put(g, new ArrayList<String>());
			}
			List<String> actionSet = this.actions.get(g);
			for (String intG : interactions.get(g)) {
				if (genes.contains(intG))
					actionSet.add(intG);
			}
		}
		for (String g: this.genes) {
			if (this.geneVars.get(g).size() == 3) {
				double combined_score = 0.0;
				String[] set1 = this.geneVars.get(g).get(0).split("\t", -1);
				String[] set2 = this.geneVars.get(g).get(1).split("\t", -1);
				String[] set3 = this.geneVars.get(g).get(2).split("\t", -1);
				String first_var = "";
				String second_var = "";
				String third_var = "";
				String s = "";
				for (int x=0; x < 7; x++){
					first_var += set1[x] + "\t";
					second_var += set2[x] + "\t";
					third_var += set3[x] + "\t";
				}
				first_var += set1[9] + "\t";
				second_var += set2[9] + "\t";
				third_var += set3[9] + "\t";
				first_var += set1[10];
				second_var += set2[10];
				third_var += set3[10];
				double first_pred = Double.parseDouble(set1[10]);
				double second_pred = Double.parseDouble(set2[10]);
				double third_pred = Double.parseDouble(set3[10]);
				combined_score = first_pred + second_pred + third_pred;
				s = first_var + "\t" + second_var + "\t" + third_var;
				if (data.size() < topCount) { //add to list directly
					DataResult mydata = new DataResult(s, combined_score);
					data.add(mydata);
					dataScores.add(combined_score);
					combo.add(s);
				}
				else { 
					double minVal = Collections.min(dataScores);
					if ( minVal < combined_score ) {
						int minIndex = dataScores.indexOf(minVal);
						DataResult mydata = new DataResult(s, combined_score);
						data.set(minIndex, mydata);
						dataScores.set(minIndex,combined_score); 
						combo.set(minIndex, s);
					}
				}
			}
			//process all combinations of genes interacting!
			int count = this.actions.get(g).size();
			String g1 = g;
			for (int i = 0; i < count; i++) {
				String g2 = this.actions.get(g).get(i);
				for (int j = i+1; j < count; j++) {
					String g3 = this.actions.get(g).get(j);
					for (String var1: this.geneVars.get(g1)) {
						for (String var2: this.geneVars.get(g2)) {
							for (String var3: this.geneVars.get(g3)) {
								if ((!var1.equals(var2)) && (!var2.equals(var3)) && (!var1.equals(var3))) {
									double combined_score = 0.0;
									String[] set1 = var1.split("\t", -1);
									String[] set2 = var2.split("\t", -1);
									String[] set3 = var3.split("\t", -1);
									String first_var = "";
									String second_var = "";
									String third_var = "";
									String s = "";
									for (int x=0; x < 7; x++){
										first_var += set1[x] + "\t";
										second_var += set2[x] + "\t";
										third_var += set3[x] + "\t";
									}
									first_var += set1[9] + "\t";
									second_var += set2[9] + "\t";
									third_var += set3[9] + "\t";
									first_var += set1[10];
									second_var += set2[10];
									third_var += set3[10];
									double first_pred = Double.parseDouble(set1[10]);
									double second_pred = Double.parseDouble(set2[10]);
									double third_pred = Double.parseDouble(set3[10]);
									combined_score = first_pred + second_pred + third_pred;
									s = first_var + "\t" + second_var + "\t" + third_var;
									boolean found = checkPermutation(first_var, second_var, third_var, combo);
									if (!found) {
										if (data.size() < topCount) { 
											DataResult mydata = new DataResult(s, combined_score);
											data.add(mydata);
											dataScores.add(combined_score);
											combo.add(s);
										}
										else { 
											double minVal = Collections.min(dataScores);
											if ( minVal < combined_score ) {
												int minIndex = dataScores.indexOf(minVal);
												DataResult mydata = new DataResult(s, combined_score);
												data.set(minIndex, mydata);
												dataScores.set(minIndex,combined_score); 
												combo.set(minIndex, s);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		Collections.sort(data, Collections.reverseOrder());
		if (topCount < data.size()) {
			for (int i = 0; i < topCount; i++) {
				out.println(data.get(i).s + "\t" + data.get(i).r);
			}
		}
		else {
			for (int i = 0; i < data.size(); i++) {
				out.println(data.get(i).s + "\t" + data.get(i).r);
			}
		}
        out.flush();
        out.close();
		}
}
	public void toolCombine(String fileName, Pseudograph<String, DefaultEdge> actions, int param) throws Exception {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName + ".oligogenic"), 1073741824));
		for (int i = 1; i<=param; i++) {
		out.print("Chr"+i+"\tStart"+i+"\tRef"+i+"\tAlt"+i+"\tGT"+i+"\tGene"+i+"\tCADD\tSim_Score\tPrediction_Score"+i+"\t");
		}
		out.println("Combined_Score");
		List<String> vars = new ArrayList<String>();
		try(BufferedReader br = Files.newBufferedReader(Paths.get(fileName + ".top"))) {
			String line;
			while((line = br.readLine()) != null) {
				 vars.add(line);
			}
			br.close(); 
		}
		//process combinations
		List<DataResult> data = new ArrayList<DataResult>();
		List<List<String>> combinations = getCombinations(param, vars);
		for (List<String> l : combinations) {
			Set<String> genes = new HashSet<String>();
			for (String s : l) {
				String gene = s.split("\t", -1)[5];
				genes.add(gene);
			}
			//do connectivity check
			AsSubgraph<String, DefaultEdge> mysub = new AsSubgraph(actions, genes);
			//System.out.println(mysub.toString());
			ConnectivityInspector cc = new ConnectivityInspector(mysub);
			if (cc.isGraphConnected()) {
				double combined_score = 0.0;
				String mycombination = "";
				//accumulate prediction score
				for (String s : l) {
					if (!mycombination.isEmpty())
						mycombination += "\t";
					combined_score += Double.parseDouble(s.split("\t", -1)[10]);
					String[] set = s.split("\t", -1);
					for (int x=0; x < 7; x++) {
						mycombination += set[x] + "\t";
					}
					mycombination += set[9] + "\t";
					mycombination += set[10];
				}
				DataResult mydata = new DataResult(mycombination, combined_score);
				data.add(mydata);
			}
		}
		Collections.sort(data, Collections.reverseOrder());
		for (int i = 0; i < data.size(); i++) {
			out.println(data.get(i).s + "\t" + data.get(i).r);
		}
		out.flush();
		out.close();
	}
	public void toCSV(String fileName, Set<String> topPhenos, String inh) throws Exception {
        PrintWriter out = new PrintWriter(new BufferedWriter(
           new FileWriter(fileName + ".csv"), 104857600));

        out.print("TYPE,geno,cadd,gwava,dann,sim");
        for (String topPheno: this.topLevelPhenotypes) {
            out.print("," + topPheno);
        }
        out.print(",inh_0,inh_1,inh_2,inh_3,CADD_impute,GWAVA_impute,Similarity_score_impute,DANN_impute");
		out.println();
        try(BufferedReader br = Files.newBufferedReader(Paths.get(fileName + ".out"))) {
            boolean cs = true;
            String line, geno, cadd, dann, gwava, sim, cadd_imp, dann_imp, gwava_imp, sim_imp, inh_0, inh_1, inh_2, inh_3;			
            while((line = br.readLine()) != null) {
				cadd_imp = "0";
				dann_imp = "0";
				gwava_imp = "0";
				sim_imp = "0";
				inh_0 = "0";
				inh_1 = "0";
				inh_2 = "0";
				inh_3 = "0";
				geno = "0";
                String[] items = line.split("\t", -1);
                for(int i = 0; i < items.length; i++) {
                   if (items[i].equals(".")) items[i] = "?";
                }
				//geno,cadd,gwava,dann,sim
				cadd = items[6];
				dann = items[8];
				gwava = items[7];
				sim = items[9];
				//modify genotype
				if (items[4].equals("1/0")) {
                    items[4] = "0/1";
					geno = "0";
                } else if (items[4].equals("1/1")) {
					geno = "1";
				} else if (items[4].equals("0/1")) {
					geno = "0";
				}
				if (cadd.equals("?")) {
					cadd_imp = "1";
					cadd = "24.178707394956216";
				}
				if (dann.equals("?")) {
					dann_imp = "1";
					dann = "0.90596700944414021";
				}
				if (gwava.equals("?")) {
					gwava_imp = "1";
					gwava = "0.46623976853229876";
				}
				if (sim.equals("?")) {
					sim_imp = "1";
					sim = "0.78501856453042218";
				}
				if (inh.equals("0")) {
					inh_0 = "1";
				} else if (inh.equals("1")) {
					inh_1 = "1";
				} else if (inh.equals("2")) {
					inh_2 = "1";
				} else {
					inh_3 = "1";
				}
				//print new NN-friendly format
				out.print("0");
				out.print("," + geno);
				out.print("," + cadd);
				out.print("," + gwava);
				out.print("," + dann);
				out.print("," + sim);
                for (String topPheno: this.topLevelPhenotypes) {
                    if (topPhenos.contains(topPheno)) {
                        out.print(",1");
                    } else {
                        out.print(",0");
                    }
                }
                out.print("," + inh_0);
				out.print("," + inh_1);
				out.print("," + inh_2);
				out.print("," + inh_3);
				out.print("," + cadd_imp);
				out.print("," + gwava_imp);
				out.print("," + sim_imp);
				out.print("," + dann_imp);

                out.println();
            }
            br.close();
        }
        out.flush();
        out.close();
    }

    class DataResult implements Comparable<DataResult> {

        String s;
        double r;

        public DataResult(String s, double r){
            this.s = s;
            this.r = r;
        }

        @Override
        public int compareTo(DataResult dr) {
            return Double.compare(this.r, dr.r);
        }
    }

	public static <String> List<List<String>> getCombinations(int k, List<String> list) {
    List<List<String>> combinations = new ArrayList<List<String>>();
    if (k == 0) {
        combinations.add(new ArrayList<String>());
        return combinations;
    }
    for (int i = 0; i < list.size(); i++) {
        String element = list.get(i);
        List<String> rest = getSublist(list, i+1);
        for (List<String> previous : getCombinations(k-1, rest)) {
            previous.add(element);
            combinations.add(previous);
        }
    }
    return combinations;
}

	public static boolean checkPermutation(String first_var, String second_var, String third_var, List<String> combo) {
		String s1,s2,s3,s4,s5,s6;
		s1 = first_var + "\t" + second_var + "\t" + third_var;
		s2 = first_var + "\t" + third_var + "\t" + second_var;
		s3 = second_var + "\t" + third_var + "\t" + first_var;
		s4 = second_var + "\t" + first_var + "\t" + third_var;
		s5 = third_var + "\t" + first_var + "\t" + second_var;
		s6 = third_var + "\t" + second_var + "\t" + first_var;

		if (combo.contains(s1))
			return true;
		else if (combo.contains(s2))
			return true;
		else if (combo.contains(s3))
			return true;
		else if (combo.contains(s4))
			return true;
		else if (combo.contains(s5))
			return true;
		else if (combo.contains(s6))
			return true;
		else
			return false;

}

	public static <String> List<String> getSublist(List<String> list, int i) {
		List<String> sublist = new ArrayList<String>();
		for (int j = i; j < list.size(); j++) {
			sublist.add(list.get(j));
		}
		return sublist;
	}
}
