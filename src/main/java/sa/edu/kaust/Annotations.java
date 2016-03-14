import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.nio.*;
import java.nio.file.*;
import java.util.function.*;

import htsjdk.tribble.readers.TabixReader;


public class Annotations {
    Properties props;
    boolean[] busy;
    int tabixN;
    int cur = 0;

    public Annotations(Properties props) throws Exception {
        this.props = props;
        this.tabixN = 30;
    }

    public void readGzip() throws Exception {
        Map<String, List<String> > map = new HashMap<String, List<String> >();
        try(BufferedReader br = Files.newBufferedReader(Paths.get("data/1maxat.txt"))) {
            String line;
            while((line = br.readLine()) != null) {
                if (!line.startsWith("rs")) {
                    continue;
                }
                map.put(line, new ArrayList<String>());
            }
        }

        InputStream fileStream = new FileInputStream(this.props.getProperty("dbNSFPPath"));
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
        BufferedReader buffered = new BufferedReader(decoder);
        String line;
        while((line = buffered.readLine()) != null) {
            String[] items = line.split("\t");
            if (map.containsKey(items[6])) {
                map.get(items[6]).add(line);
                System.out.println("ok");
            }
        }
        PrintWriter out = new PrintWriter(new BufferedWriter(
            new FileWriter("data/output-adeeb.txt"), 1073741824));

        for (String key: map.keySet()) {
            for (String l: map.get(key)) {
                out.println(l);
            }
        }
        out.close();

    }
    public Map<String, Double> getAnnotations(String vcfFilePath) throws Exception {
        Map<String, Double> result = new HashMap<String, Double>();
        PrintWriter out = new PrintWriter(new BufferedWriter(
            new FileWriter("data/output.txt"), 1073741824));
        ArrayList<String> dataList = new ArrayList<String>();
        try(BufferedReader br = Files.newBufferedReader(Paths.get(vcfFilePath))) {
            String line;
            while((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
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
                    String start = items[1];
                    String rsId = items[2];
                    String ref = items[3];
                    String alt = items[4];
                    String phred = items[5];
                    String genotype = items[9];
                    // Computing end position
                    String end = "NA";
                    int refLength = ref.length();
                    int altLength = alt.length();

                    if (refLength == altLength) {
                        end = start;
                    } else if (refLength < altLength) {
                        end = Integer.toString(Integer.parseInt(start) + 1);
                    } else {
                        start = Integer.toString(Integer.parseInt(start) + 1);
                        end = Integer.toString(
                            Integer.parseInt(start) + refLength - altLength);
                    }
                    String query = chr + ":" + start + "-" + end;
                    // System.out.println(query);
                    int curi = that.cur;
                    synchronized(this) {
                        while(that.busy[that.cur]) {
                            that.cur++;
                            if (that.cur == that.tabixN) that.cur = 0;
                        }
                        that.busy[that.cur] = true;
                        curi = that.cur;
                    }
                    // TabixReader tabix = that.dbnsfpTabixes[curi];
                    // TabixReader.Iterator iter = tabix.query(query);
                    // String s;
                    // ArrayList<String[]> list = new ArrayList<String[]>();
                    // while (iter != null && (s = iter.next()) != null) {
                    //     String[] results = s.split("\t");
                    //     String geneName = results[11];
                    //     String siftScore = that.avg(results[23]);
                    //     String polyphenHDIVScore = that.avg(results[29]);
                    //     String lrtScore = that.avg(results[35]);
                    //     String mutationTasterScore = that.avg(results[39]);
                    //     String fathmmScore = that.avg(results[49]);
                    //     String proveanScore = that.avg(results[52]);
                    //     String vset3Score = that.avg(results[57]);
                    //     String dannScore = that.avg(results[62]);
                    //     String metaSVMScore = that.avg(results[68]);
                    //     String metaLRScore = that.avg(results[71]);
                    //     String gerpNR = that.avg(results[87]);
                    //     String gerpRS = that.avg(results[88]);
                    //     String kgpFreq = that.avg(results[102]);
                    //     String huvecFitConsScore = that.avg(results[84]);
                    //     String huvecConfidenceValue = that.avg(results[86]);
                    //     list.add(new String[]{});
                    // }
                    // tabix = that.caddTabixes[curi];
                    // iter = tabix.query(query);
                    // String caddScore = ".";
                    // String caddGene = ".";
                    // double sum = 0;
                    // int n = 0;
                    // while (iter != null && (s = iter.next()) != null) {
                    //     String[] results = s.split("\t");
                    //     String cg = results[95];
                    //     String cs = results[115];
                    //     if (caddGene.equals(".") && !cg.equals("NA") ) {
                    //         caddGene = cg;
                    //     }
                    //     sum += Double.parseDouble(cs);
                    //     n++;
                    // }
                    // if (n > 0) {
                    //     caddScore = Double.toString(sum / n);
                    // }

                    // tabix = that.gwavaTabixes[curi];
                    // iter = tabix.query(query);
                    // String gwavaScore = ".";
                    // if (iter != null && (s = iter.next()) != null) {
                    //     String[] results = s.split("\t");
                    //     sum = 0;
                    //     sum += Double.parseDouble(results[4]);
                    //     sum += Double.parseDouble(results[5]);
                    //     sum += Double.parseDouble(results[6]);
                    //     sum /= 3;
                    //     gwavaScore = Double.toString(sum);
                    // }

                    // that.busy[curi] = false;

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "";
            }
        };
        Arrays.parallelSetAll(data, annotation);
        out.close();
        return result;

    }

    public String avg(String values) {
        return this.avg(values, ";");
    }

    public String avg(String values, String delimiter) {
        if (values.trim().equals(".")) return ".";
        String[] v = values.split(delimiter);
        if (v.length == 1) return values;
        double sum = 0;
        int n = 0;
        for (int i = 0; i < v.length; i++) {
            if(!v[i].equals(".")){
                sum += Double.parseDouble(v[i]);
                n++;
            }
        }
        if (n > 0) {
            return Double.toString(sum / n);
        }
        return ".";
    }

}
