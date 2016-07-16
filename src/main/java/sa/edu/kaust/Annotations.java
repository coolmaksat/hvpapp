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
    TabixReader[] tabixes;
    TabixReader[] caddTabixes;
    TabixReader[] dannTabixes;
    TabixReader[] gwavaTabixes;
    Map<String, String> ccdsGenes;

    public Annotations(Properties props) throws Exception {
        this.props = props;
        this.tabixN = 50;
        this.tabixes = new TabixReader[this.tabixN];
        this.busy = new boolean[this.tabixN];
        for (int i = 0; i < this.tabixN; i++) {
            this.tabixes[i] = new TabixReader(this.props.getProperty("dbPath"));
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


    public void readGzip() throws Exception {
        Map<String, List<String> > map = new HashMap<String, List<String> >();
        try(BufferedReader br = Files.newBufferedReader(Paths.get("data/1maxat.txt"))) {
            String line;
            while((line = br.readLine()) != null) {
                map.put(line.trim(), new ArrayList<String>());
            }
        }
        PrintWriter out = new PrintWriter(new BufferedWriter(
            new FileWriter("data/output-adeeb.txt"), 1073741824));
        InputStream fileStream = new FileInputStream("data/db/dbNSFP.gz");
        InputStream gzipStream = new GZIPInputStream(fileStream);
        Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
        BufferedReader buffered = new BufferedReader(decoder);
        String line = buffered.readLine();
        out.println(line);
        while((line = buffered.readLine()) != null) {
            String[] items = line.split("\t", -1);
            if (!items[6].equals(".") && map.containsKey(items[6])) {
                out.println(line);
            }
        }
        out.close();

    }

    public void readDbFile() throws Exception {
        String filePath = "data/db/cadd.txt";
        FileChannel channel = new RandomAccessFile(filePath, "rw").getChannel();
        int bufferSize = Integer.MAX_VALUE / 2;
        int total = (int)(channel.size() / bufferSize);
        if (channel.size() % bufferSize > 0) {
            total++;
        }
        MappedByteBuffer[] buffers = new MappedByteBuffer[total];
        long start = 0;
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = channel.map(FileChannel.MapMode.READ_WRITE, start, bufferSize);
            start += bufferSize;
        }
        StringBuilder sb = new StringBuilder();
        char c;
        Map<String, Double> cadd = new HashMap<String, Double>();
        for (int i = 0; i < buffers.length; i++) {
            sb.append(Charset.defaultCharset().decode(buffers[i]).toString());
            int l = sb.lastIndexOf("\n");
            String[] lines = sb.substring(0, l).split("\n");
            String t = sb.substring(l + 1);
            sb = new StringBuilder(t);
            System.out.println(sb.length());
            for (String line: lines){
                if (line.startsWith("#")) {
                    continue;
                }
                String[] items = line.split("\t");
                String key = items[0] + "_" + items[1] + "_" + items[2] + "_" + items[3];
                double score = Double.parseDouble(items[7]);
                cadd.put(key, score);
            }
        }
    }

    public Map<String, Double> getAnnotations(String vcfFilePath, String mode, String model) throws Exception {
        Map<String, Double> result = new HashMap<String, Double>();
        PrintWriter out = new PrintWriter(new BufferedWriter(
            new FileWriter(vcfFilePath + ".out"), 1073741824));
        ArrayList<String> dataList = new ArrayList<String>();
        try(BufferedReader br = Files.newBufferedReader(Paths.get(vcfFilePath))) {
            String line;
            while((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] items = line.split("\t");
                String genotype = items[9].split(":")[0];
                if (!(genotype.equals("0/1") || genotype.equals("1/1"))) {
                    continue;
                }
                if (mode.equals("recessive") && genotype.equals("0/1")) {
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
                    String pos = items[1];
                    String rsId = items[2];
                    String ref = items[3];
                    String alt = items[4];
                    String genotype = items[9].split(":")[0];
                    // String ref = items[3];
                    // String alt = items[4];

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

                    TabixReader tabix = that.tabixes[curi];
                    TabixReader.Iterator iter = tabix.query(query);
                    String caddScore = ".";
                    String caddGene = ".";
                    String dannScore = ".";
                    String gwavaScore = ".";
                    String type = ".";
                    String s;
                    boolean found = false;
                    while (iter != null && (s = iter.next()) != null) {
                        String[] results = s.split("\t");
                        if (results[2].equals(ref) && results[3].equals(alt)) {
                            caddScore = results[6];
                            gwavaScore = results[7];
                            dannScore = results[8];
                            caddGene = results[5];
                            if (caddGene.startsWith("CCDS") && that.ccdsGenes.containsKey(caddGene)) {
                                caddGene = that.ccdsGenes.get(caddGene);
                            }
                            type = results[4];
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        tabix = that.caddTabixes[curi];
                        iter = tabix.query(query);
                        while (iter != null && (s = iter.next()) != null) {
                            String[] results = s.split("\t");
                            if (results[2].equals(ref) && results[3].equals(alt)) {
                                caddScore = results[7];
                                if (!results[6].equals("NA")) {
                                    caddGene = results[6];
                                    if (caddGene.startsWith("CCDS") && that.ccdsGenes.containsKey(caddGene)) {
                                        caddGene = that.ccdsGenes.get(caddGene);
                                    }
                                }
                                if (results[4].equals("CodingTranscript")) {
                                    type = "NonCoding";
                                } else {
                                    type = "Coding"
                                }
                                break;
                            }
                        }

                        tabix = that.dannTabixes[curi];
                        iter = tabix.query(query);
                        while (iter != null && (s = iter.next()) != null) {
                            String[] results = s.split("\t");
                            if (results[2].equals(ref) && results[3].equals(alt)) {
                                dannScore = results[4];
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
                        chr, pos, ref, alt, caddGene, caddScore,
                        gwavaScore, dannScore, genotype);
                    return ret + "::" + type;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return data[i];
            }
        };
        Arrays.parallelSetAll(data, annotation);
        for (String res: data) {
            String[] r = res.split("::");
            if (r[1].equals(model)) {
                out.println(r[0]);
            }
        }
        out.close();
        return result;

    }

}
