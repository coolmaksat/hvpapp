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

    public Classification(Properties props) throws Exception {
        // Loading the saved classifier
        this.modelFile = props.getProperty("model");
        this.topLevelPhenotypes = props.getProperty("topLevelPhenotypes").split(", ");
    }


    public void toolClassify(String fileName) throws Exception {
        //FileReader fr = new FileReader(fileName + ".csv");
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

	public void toolDigenic(String fileName, Map<String, List<String> > interactions, int param) throws Exception {
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
		//process combinations
		List<DataResult> data = new ArrayList<DataResult>();
		//int index = 0;
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
				if ((interactions.get(first_gene).contains(second_gene)) || first_gene.equals(second_gene)){
					combined_score = first_pred + second_pred;
					s = first_var + "\t" + second_var;
					DataResult mydata = new DataResult(s, combined_score);
					data.add(mydata);
				}
			}
		}
		Collections.sort(data, Collections.reverseOrder());
		for (int i = 0; i < data.size(); i++) {
            out.println(data.get(i).s + "\t" + data.get(i).r);
        }
        out.flush();
        out.close();
		}
    }

	public void toolTrigenic(String fileName, Map<String, List<String> > interactions, int param) throws Exception {
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
		//int index = 0;
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
				for (int k=j+1; k < vars.size(); k++) {
					String[] set3 = vars.get(k).split("\t", -1);
					String third_var = "";
						for (int z=0; z < 7; z++){
						third_var += set3[z] + "\t";
						}
					third_var += set3[9] + "\t";
					third_var += set3[10];
					String third_gene = vars.get(k).split("\t", -1)[5];
					double third_pred = Double.parseDouble(vars.get(k).split("\t", -1)[10]);
					//check if both genes interact
					//if genes are identical
					if ((first_gene.equals(second_gene)) && (second_gene.equals(third_gene))) {
						combined_score = first_pred + second_pred + third_pred;
						s = first_var + "\t" + second_var + "\t" + third_var;
						DataResult mydata = new DataResult(s, combined_score);
						data.add(mydata);
					}
					else {
						if ((interactions.get(first_gene).contains(second_gene)) && (interactions.get(first_gene).contains(third_gene))){
							combined_score = first_pred + second_pred + third_pred;
							s = first_var + "\t" + second_var + "\t" + third_var;
							DataResult mydata = new DataResult(s, combined_score);
							data.add(mydata);
						}
						else if ((interactions.get(first_gene).contains(second_gene)) && (interactions.get(second_gene).contains(third_gene))){
							combined_score = first_pred + second_pred + third_pred;
							s = first_var + "\t" + second_var + "\t"+ third_var;
							DataResult mydata = new DataResult(s, combined_score);
							data.add(mydata);
						}
						else if ((interactions.get(first_gene).contains(third_gene)) && (interactions.get(third_gene).contains(second_gene))){
							combined_score = first_pred + second_pred + third_pred;
							s = first_var + "\t" + second_var + "\t"+ third_var;
							DataResult mydata = new DataResult(s, combined_score);
							data.add(mydata);
						}
					}
				}
			}
		}
		Collections.sort(data, Collections.reverseOrder());
		for (int i = 0; i < data.size(); i++) {
            out.println(data.get(i).s + "\t" + data.get(i).r);
        }
        out.flush();
        out.close();
		}
    }
	public void toolCombine(String fileName, UndirectedGraph<String, DefaultEdge> actions, int param) throws Exception {
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName + ".digenic"), 1073741824));
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
	public static <String> List<String> getSublist(List<String> list, int i) {
		List<String> sublist = new ArrayList<String>();
		for (int j = i; j < list.size(); j++) {
			sublist.add(list.get(j));
		}
		return sublist;
	}
}
