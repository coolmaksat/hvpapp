package sa.edu.kaust;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.nio.*;
import java.nio.file.*;

import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.Instance;


public class Classification {

    public String dataRoot = "";
    public String resultRoot = "";
    public String modelName = "";
    public String arffFilesPath = "";
    RandomForest cls;
    public String[] topLevelPhenotypes;

    public Classification(Properties props, String model) throws Exception {
        // Loading the saved classifier
        String rfModelFile = props.getProperty("model" + model);
        this.dataRoot = props.getProperty("dataRoot");
        this.resultRoot = props.getProperty("resultRoot");
        this.arffFilesPath = props.getProperty("arffFiles");
        this.cls = (RandomForest)weka.core.SerializationHelper.read(rfModelFile);
        this.modelName = Paths.get(rfModelFile).getFileName().toString().split("\\.")[0] + "/";
        // Files.createDirectories(Paths.get(this.resultRoot + this.modelName));
        this.topLevelPhenotypes = props.getProperty("topLevelPhenotypes").split(", ");
    }

    public void toolClassify(String fileName) throws Exception {
        FileReader fr = new FileReader(fileName + ".arff");
        Instances is = new Instances(fr);
        is.setClassIndex(0);
        Double[] results = new Double[is.numInstances()];
        Classification that = this;
        IntFunction<Double> clsfy = new IntFunction<Double>() {
            @Override
            public Double apply(int i) {
                Instance instance = is.instance(i);
                try {
                    double[] result = that.cls.distributionForInstance(instance);
                    return result[0];
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 0.0;
            }
        };
        Arrays.parallelSetAll(results, clsfy);
        DataResult[] data = new DataResult[results.length];
        try(BufferedReader br = Files.newBufferedReader(Paths.get(fileName + ".out"))) {
            String line;
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
        out.close();
    }

    public void classifyFiles(int ind) throws Exception {
        String dataRoot = this.resultRoot;
        List<String> list = new ArrayList<String>();
        try(BufferedReader br = Files.newBufferedReader(Paths.get(this.arffFilesPath))) {
            String line;
            while((line = br.readLine()) != null) {
                list.add(line);
            }
            br.close();
        }
        String[] fileNames = list.toArray(new String[list.size()]);
        String fileName = fileNames[ind];
        if (!Files.exists(Paths.get(dataRoot + this.modelName + fileName + ".res")) || Files.size(Paths.get(dataRoot + this.modelName + fileName + ".res")) == 0) {
            System.out.println("Starting classification for " + fileName);
            this.classify(fileName);
            System.out.println("Classification finished for " + fileName);
        } else {
            System.out.println("Not running classification for " + fileName);
        }
    }

    public void classify(int ind) throws Exception {
        String dataRoot = this.resultRoot;
        DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(dataRoot), "*.arff");
        List<String> list = new ArrayList<String>();
        for (Path filePath: files) {
            String fileName = filePath.getFileName().toString();
            list.add(fileName);
        }
        String[] fileNames = list.toArray(new String[list.size()]);
        String fileName = fileNames[ind];
        if (!Files.exists(Paths.get(dataRoot + this.modelName + fileName + ".res")) || Files.size(Paths.get(dataRoot + this.modelName + fileName + ".res")) == 0) {
            System.out.println("Starting classification for " + fileName);
            this.classify(fileName);
            System.out.println("Classification finished for " + fileName);
        } else {
            System.out.println("Not running classification for " + fileName);
        }
    }


    public void classify(String fileName) throws Exception {
        String dataRoot = this.resultRoot;
        FileReader fr = new FileReader(dataRoot + fileName);
        Instances is = new Instances(fr);
        is.setClassIndex(0);
        String[] results = new String[is.numInstances()];
        Classification that = this;
        IntFunction<String> clsfy = new IntFunction<String>() {
            @Override
            public String apply(int i) {
                Instance instance = is.instance(i);
                try {
                    double[] result = that.cls.distributionForInstance(instance);
                    return result[0] + " " + result[1];
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "";
            }
        };
        Arrays.parallelSetAll(results, clsfy);
        PrintWriter out = new PrintWriter(new BufferedWriter(
            new FileWriter(dataRoot + this.modelName + fileName + ".res"), 104857600));
        for (int i = 0; i < results.length; i++) {
            out.println(results[i]);
        }
        out.close();
    }

    public void classifyAll() throws Exception {
        String dataRoot = this.resultRoot;
        DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(dataRoot), "*.arff");
        List<String> list = new ArrayList<String>();
        for (Path filePath: files) {
            String fileName = filePath.getFileName().toString();
            if (!Files.exists(Paths.get(dataRoot + this.modelName + fileName + ".res")) || Files.size(Paths.get(dataRoot + this.modelName + fileName + ".res")) == 0) {
                list.add(fileName);
            }
        }
        String[] fileNames = list.toArray(new String[list.size()]);
        Classification that = this;
        IntFunction<String> clsfy = new IntFunction<String>() {
            @Override
            public String apply(int i) {
                String fileName = list.get(i);
                try {
                    System.out.println("Classifying file " + fileName);
                    that.classify(fileName);
                    System.out.println("Classifying file " + fileName + " finished");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return fileName;
            }
        };

        Arrays.parallelSetAll(fileNames, clsfy);

    }

    public void toArffAll() throws Exception {
        String dataRoot = this.dataRoot;
        String resultRoot = this.resultRoot;
        DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(dataRoot), "*.vcf");
        List<String> list = new ArrayList<String>();
        for (Path filePath: files) {
            String fileName = filePath.getFileName().toString();
            if (!Files.exists(Paths.get(resultRoot + fileName + ".arff"))  || Files.size(Paths.get(resultRoot + fileName + ".arff")) == 0) {
                list.add(fileName);
            }
        }
        String[] fileNames = list.toArray(new String[list.size()]);
        Classification that = this;
        IntFunction<String> arff = new IntFunction<String>() {
            @Override
            public String apply(int i) {
                String fileName = list.get(i);
                try {
                    System.out.println("Processing file " + fileName);
                    that.toArff(fileName);
                    System.out.println("Processing file " + fileName + " finished");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return fileName;
            }
        };

        Arrays.parallelSetAll(fileNames, arff);

    }

    public void toArff(String fileName, Set<String> topPhenos, String inh) throws Exception {
        PrintWriter out = new PrintWriter(new BufferedWriter(
           new FileWriter(fileName + ".arff"), 104857600));
        out.println("@relation " + fileName);
        out.println();
        out.println("@attribute TYPE {CASE,CTRL}");
        for (String topPheno: this.topLevelPhenotypes) {
            out.println("@attribute " + topPheno + " numeric");
        }
        out.println("@attribute inh numeric");
        out.println("@attribute cadd numeric");
        out.println("@attribute gwava numeric");
        out.println("@attribute sim numeric");
        out.println("@attribute dann numeric");
        out.println("@attribute geno {1/1,0/1}");
        out.println("");
        out.println("@data");
        try(BufferedReader br = Files.newBufferedReader(Paths.get(fileName + ".out"))) {
            boolean cs = true;
            String line;
            while((line = br.readLine()) != null) {
                String[] items = line.split("\t", -1);
                for(int i = 0; i < items.length; i++) {
                   if (items[i].equals(".")) items[i] = "?";
                }
                out.print("CTRL");
                for (String topPheno: this.topLevelPhenotypes) {
                    if (topPhenos.contains(topPheno)) {
                        out.print(",1");
                    } else {
                        out.print(",0");
                    }
                }
                out.print("," + inh);
                out.print("," + items[6]); // CADD
                out.print("," + items[7]); // GWAVA
                out.print("," + items[9]); // SIM
                out.print("," + items[8]); // DANN
                if (items[4].equals("1/0")) {
                    items[4] = "0/1";
                }
                out.print("," + items[4]); // geno
                out.println();
            }
            br.close();
        }
        out.close();
    }

    public void toArff(String fileName) throws Exception {
        String dataRoot = this.dataRoot;
        String resultRoot = this.resultRoot;
        PrintWriter out = new PrintWriter(new BufferedWriter(
           new FileWriter(resultRoot + fileName + ".arff"), 104857600));
        out.println("@relation " + fileName);
        out.println();
        out.println("@attribute TYPE {CASE,CTRL}");
        for (String topPheno: this.topLevelPhenotypes) {
            out.println("@attribute " + topPheno + " numeric");
        }
        // out.println("@attribute inh numeric");
        out.println("@attribute cadd numeric");
        out.println("@attribute gwava numeric");
        out.println("@attribute sim numeric");
        out.println("@attribute dann numeric");
        out.println("@attribute geno {1/1,0/1}");
        out.println("@attribute moi_rz numeric");
        out.println("");
        out.println("@data");
       try(BufferedReader br = Files.newBufferedReader(Paths.get(dataRoot + fileName))) {
           String line = br.readLine(); // reading the header
           boolean cs = true;
           while((line = br.readLine()) != null) {
               String[] items = line.split("\t", -1);
               if (!(items[58].equals("1/1") || items[58].equals("0/1"))) {
                   continue;
               }
               for(int i = 0; i < items.length; i++) {
                   if (items[i].equals(".")) items[i] = "?";
               }
               out.print("CTRL");
               for(int i = 0; i < items.length; i++) {
                   out.print("," + items[i]);
               }
               // out.print("," + items[57]); // CADD
               // out.print("," + items[58]); // GWAVA
               // out.print("," + items[60]); // SIM
               // out.print("," + items[59]); // DANN
               // out.print("," + items[56]); // geno
               out.println();
           }
           br.close();
       }
       out.close();
    }

    public String sortResults(String fileName) throws Exception {
        String resDataRoot = this.resultRoot + this.modelName + "/";
        List<Result> resList = new ArrayList<Result>();
        int c = resList.size() - 1;
        try(BufferedReader br = Files.newBufferedReader(Paths.get(resDataRoot + fileName + ".arff.res"))) {
            String line = null;
            int i = 0;
            while((line = br.readLine()) != null) {
                double d = Double.parseDouble(line.split(" ")[0]);
                resList.add(new Result(i, d));
                i++;
            }
            br.close();
        }
        Collections.sort(resList, Collections.reverseOrder());
        for (int i = 0; i < resList.size(); i++) {
            if (resList.get(i).i == resList.size() - 1) {
                return i + " " + resList.get(i).d;
            }
        }
        return "";
    }

    public void sortAll() throws Exception {
        String dataRoot = this.dataRoot;
        DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(dataRoot), "*.vcf");
        List<String> list = new ArrayList<String>();
        for (Path filePath: files) {
            String fileName = filePath.getFileName().toString();
            list.add(fileName);
        }
        String[] fileNames = list.toArray(new String[list.size()]);
        Classification that = this;
        IntFunction<String> sort = new IntFunction<String>() {
            @Override
            public String apply(int i) {
                String fileName = list.get(i);
                try {
                    System.out.println("Processing file " + fileName);
                    fileName = fileName + " " + that.sortResults(fileName);
                    System.out.println("Processing file " + fileName + " finished");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return fileName;
            }
        };

        Arrays.parallelSetAll(fileNames, sort);

        PrintWriter out = new PrintWriter(new BufferedWriter(
            new FileWriter(this.resultRoot + this.modelName.substring(0, this.modelName.length() - 1) + ".res"), 104857600));
        for (String res: fileNames) {
            out.println(res);
        }
        out.close();

    }

    public void getMissingResults() {

    }

    class Result implements Comparable<Result> {

        double d;
        int i;

        public Result(int i, double d){
            this.i = i;
            this.d = d;
        }

        @Override
        public int compareTo(Result r) {
            return Double.compare(this.d, r.d);
        }
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
}
