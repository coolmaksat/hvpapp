package sa.edu.kaust;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.function.*;

import htsjdk.tribble.readers.TabixReader;


public class Annotations {
    Properties props;
    boolean[] busy;
    int tabixN;
    int cur = 0;
    TabixReader[] caddTabixes;
    TabixReader[] dannTabixes;
    TabixReader[] gwavaTabixes;
    Map<String, String> ccdsGenes;
    boolean all = false;
	String dataFile;

    public Annotations(Properties props) throws Exception {
        this.props = props;
        this.tabixN = 50;
        this.caddTabixes = new TabixReader[this.tabixN];
        this.dannTabixes = new TabixReader[this.tabixN];
        this.gwavaTabixes = new TabixReader[this.tabixN];
		this.dataFile = props.getProperty("annoPath");
        this.busy = new boolean[this.tabixN];
        for (int i = 0; i < this.tabixN; i++) {
            this.caddTabixes[i] = new TabixReader(this.props.getProperty("caddPath"));
            this.dannTabixes[i] = new TabixReader(this.props.getProperty("dannPath"));
            this.gwavaTabixes[i] = new TabixReader(this.props.getProperty("gwavaPath"));
        }

        this.ccdsGenes = new HashMap<String, String>();
        try(BufferedReader br = Files.newBufferedReader(Paths.get("data/ccds_gene.txt"))) {
            String line;
            while((line = br.readLine()) != null) {
                String[] items = line.split("\t", -1);
                this.ccdsGenes.put(items[1], items[0]);
            }
        }


    }

    public Map<String, Double> getAnnotations(String vcfFilePath, String outFilePath, String mode, String model, Map<String, Double> sims) throws Exception {
        Map<String, Double> result = new HashMap<String, Double>();
        PrintWriter out = new PrintWriter(new BufferedWriter(
            new FileWriter(outFilePath + ".out"), 1073741824));
        ArrayList<String> dataList = new ArrayList<String>();
		//load preannotated data
		Map<String, List<String> > anno_data =  new HashMap<String, List<String> >();
		try(BufferedReader br = Files.newBufferedReader(Paths.get(dataFile))) {
			String line;
            while((line = br.readLine()) != null) {
				String[] items = line.split("\t");
				String chr = items[0];
				String pos = items[1];
				String ref = items[2];
				String alt = items[3];
				String gene = items[4].replaceAll("_","-");
				String cadd = items[5];
				String gwava = items[6];
				String dann = items[7];
				String mykey = chr + "_" + pos;
				String myval = ref + "_" + alt + "_" + gene + "_" + cadd + "_" + gwava + "_" + dann;
				String inter = items[0];
                if (!anno_data.containsKey(mykey)) {
                    anno_data.put(mykey, new ArrayList<String>());
                }
                List<String> annoSet = anno_data.get(mykey);
                annoSet.add(myval);
			}
		}
        try(BufferedReader br = Files.newBufferedReader(Paths.get(vcfFilePath))) {
            String line;
            while((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] items = line.split("\t");
                String genotype = items[9].split(":")[0].replaceAll("\\|", "\\/");
				String alt = items[4];
                if (!(genotype.equals("1/0") || genotype.equals("0/1") || genotype.equals("1/1"))) {
                    continue;
                }
                if (mode.equals("recessive") && (genotype.equals("0/1") || genotype.equals("1/0"))) {
                    continue;
                }
				if (alt.contains(",")) {
					String[] allele = alt.split(",");
					for (String s: allele) {
						String chr = items[0];
						String pos = items[1];
						String rsid = items[2];
						String ref = items[3];
						String qual = items[5];
						String pass = items[6];
						String info = items[7];
						String format = items[8];
						String gt = items[9];
						String var = String.format(
                        "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
                        chr, pos, rsid, ref, s, qual, pass,
                        info, format, gt);
						dataList.add(var);
					}
				} else
					dataList.add(line);
            }
        }
        String[] data = dataList.toArray(new String[dataList.size()]);
        Annotations that = this;
        IntFunction<String> annotation = new IntFunction() {
            @Override
            public String apply(int i) {
                try {
                    String line = data[i];
                    String[] items = line.split("\t");
                    String chr = items[0];
					//remove chr prefix if any
					if (chr.toLowerCase().startsWith("chr"))
						chr = chr.substring(3);
                    String pos = items[1];
                    String rsId = items[2];
                    String ref = items[3];
                    String alt = items[4];
                    String genotype = items[9].split(":")[0].replaceAll("\\|", "\\/");
                    // Computing end position
                    String begin = pos;
                    String end = "NA";
                    int refLength = ref.length();
                    int altLength = alt.length();
                    if (refLength == altLength) {
                        end = begin;
                    } else if (refLength < altLength) {
                        end = Integer.toString(Integer.parseInt(begin) + 1);
                    } else {
                        begin = Integer.toString(Integer.parseInt(pos) + 1);
                        end = Integer.toString(
                            Integer.parseInt(begin) + refLength - altLength);
                    }
                    String query = chr + ":" + begin + "-" + end;
                    int curi = that.cur;
                    synchronized(this) {
                        while(that.busy[that.cur]) {
                            that.cur++;
                            if (that.cur == that.tabixN) that.cur = 0;
                        }
                        that.busy[that.cur] = true;
                        curi = that.cur;
                    }

                    TabixReader tabix;
                    TabixReader.Iterator iter;
                    String caddScore = ".";
                    String caddGene = ".";
                    String dannScore = ".";
                    String gwavaScore = ".";
                    String type = ".";
                    String s;
                    boolean found = false;
					String key = chr + "_" + begin;
					//check precomputed scores first
					if (anno_data.containsKey(key)){
						for (String a: anno_data.get(key)) {
							String[] scores = a.split("_");
							if (scores[0].equals(ref) && scores[1].equals(alt)) {
								//write the values
								caddGene = scores[2];
								caddScore = scores[3];
								gwavaScore = scores[4];
								dannScore = scores[5];
								found = true;
							}
						}
					}
                    if (!found) {
                        tabix = that.caddTabixes[curi];
                        iter = tabix.query(query);
                        double maxScore = 0.0;
                        while (iter != null && (s = iter.next()) != null) {
                            String[] results = s.split("\t", -1);
                            if (caddGene.equals(".") && !results[6].equals("NA")) {
                                caddGene = results[6];
                                if (caddGene.startsWith("CCDS") && that.ccdsGenes.containsKey(caddGene)) {
                                    caddGene = that.ccdsGenes.get(caddGene);
                                }
                            }
                            if (results[4].equals("CodingTranscript")) {
                                type = "Coding";
                            } else {
                                type = "NonCoding";
                            }
                            if (results[2].equals(ref) && results[3].equals(alt)) {
                                caddScore = results[7];
                                break;
                            } else if (ref.length() != alt.length()) {
                                maxScore = Math.max(maxScore, Double.parseDouble(results[7]));
                                caddScore = Double.toString(maxScore);
                            }
                        }

                        tabix = that.dannTabixes[curi];
                        iter = tabix.query(query);
                        maxScore = 0.0;
                        while (iter != null && (s = iter.next()) != null) {
                            String[] results = s.split("\t");
                            if (results[2].equals(ref) && results[3].equals(alt)) {
                                dannScore = results[4].replace("\\r","");
                            } else if (ref.length() != alt.length()) {
                                maxScore = Math.max(maxScore, Double.parseDouble(results[4]));
                                dannScore = Double.toString(maxScore);
                            }
                        }

                        tabix = that.gwavaTabixes[curi];
                        iter = tabix.query("chr" + query);
                        if (iter != null && (s = iter.next()) != null) {
                            String[] results = s.split("\t");
                            double sum = 0;
                            sum += Double.parseDouble(results[4]);
                            sum += Double.parseDouble(results[5]);
                            sum += Double.parseDouble(results[6]);
                            sum /= 3;
                            gwavaScore = Double.toString(sum);
                        }
                    }

                    that.busy[curi] = false;
                    String ret = String.format(
                        "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
                        chr, pos, ref, alt, genotype, caddGene,
                        caddScore, gwavaScore, dannScore);
                    return ret + "::" + type + "::" + caddGene;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return data[i];
            }
        };
        Arrays.parallelSetAll(data, annotation);
        for (String res: data) {
            String[] r = res.split("::");
            String gene = r[2];
            String simScore = ".";
            if (sims.containsKey(gene)) {
                simScore = sims.get(gene).toString();
            }
            out.println(r[0] + "\t" + simScore);
        }
        out.flush();
        out.close();

        return result;

    } 

}
