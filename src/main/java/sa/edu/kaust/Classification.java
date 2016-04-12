import java.io.*;
import java.util.*;
import java.util.function.*;
import java.nio.*;
import java.nio.file.*;

import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.Instance;


public class Classification {

    public static final String TEST_DATA_ROOT = "data/models/test/final_with_missing/";
    public String modelResultDir = "";
    RandomForest cls;

    public Classification(Properties props) throws Exception {
        // Loading the saved classifier
        String rfModelFile = props.getProperty("randomForestModelFile");
        this.cls = (RandomForest)weka.core.SerializationHelper.read(rfModelFile);
        this.modelResultDir = Paths.get(rfModelFile).getFileName().toString().split("\\.")[0] + "/";
        Files.createDirectories(Paths.get(TEST_DATA_ROOT + this.modelResultDir));
    }

    public void classify(String fileName) throws Exception {
        String dataRoot = TEST_DATA_ROOT;
        FileReader fr = new FileReader(dataRoot + fileName);
        Instances is = new Instances(fr);
        is.setClassIndex(0);
        PrintWriter out = new PrintWriter(new BufferedWriter(
            new FileWriter(dataRoot + this.modelResultDir + fileName + ".res"), 104857600));
        for (int i = 0; i < is.numInstances(); i++) {
            Instance instance = is.instance(i);
            double[] result = this.cls.distributionForInstance(instance);
            out.println(result[0] + " " + result[1]);
        }
        out.close();
    }

    public void classifyAll() throws Exception {
        String dataRoot = TEST_DATA_ROOT;
        DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(dataRoot), "*.arff");
        List<String> list = new ArrayList<String>();
        for (Path filePath: files) {
            String fileName = filePath.getFileName().toString();
            if (!Files.exists(Paths.get(dataRoot + this.modelResultDir + fileName + ".res")) || Files.size(Paths.get(dataRoot + this.modelResultDir + fileName + ".res")) == 0) {
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
        String dataRoot = TEST_DATA_ROOT;
        DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(dataRoot), "*.vcf");
        List<String> list = new ArrayList<String>();
        for (Path filePath: files) {
            String fileName = filePath.getFileName().toString();
            if (!Files.exists(Paths.get(filePath.toString() + ".arff"))) {
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
    public void toArff(String fileName) throws Exception {
        String dataRoot = TEST_DATA_ROOT;
        PrintWriter out = new PrintWriter(new BufferedWriter(
            new FileWriter(dataRoot + fileName + ".arff"), 104857600));
        out.println("@relation " + fileName);
        out.println();
        out.println("@attribute TYPE {CASE,CTRL}");
        out.println("@attribute mp1 numeric");
        out.println("@attribute mp2 numeric");
        out.println("@attribute mp3 numeric");
        out.println("@attribute mp4 numeric");
        out.println("@attribute mp5 numeric");
        out.println("@attribute mp6 numeric");
        out.println("@attribute mp7 numeric");
        out.println("@attribute mp8 numeric");
        out.println("@attribute mp9 numeric");
        out.println("@attribute mp10 numeric");
        out.println("@attribute mp11 numeric");
        out.println("@attribute mp12 numeric");
        out.println("@attribute mp13 numeric");
        out.println("@attribute mp14 numeric");
        out.println("@attribute mp15 numeric");
        out.println("@attribute mp16 numeric");
        out.println("@attribute mp17 numeric");
        out.println("@attribute mp18 numeric");
        out.println("@attribute mp19 numeric");
        out.println("@attribute mp20 numeric");
        out.println("@attribute mp21 numeric");
        out.println("@attribute mp22 numeric");
        out.println("@attribute mp23 numeric");
        out.println("@attribute mp24 numeric");
        out.println("@attribute mp25 numeric");
        out.println("@attribute mp26 numeric");
        out.println("@attribute mp27 numeric");
        out.println("@attribute mp28 numeric");
        out.println("@attribute mp29 numeric");
        out.println("@attribute mp30 numeric");
        out.println("@attribute inh numeric");
        out.println("@attribute CADD numeric");
        out.println("@attribute GWAVA numeric");
        out.println("@attribute SIM numeric");
        out.println("@attribute geno {1/1,0/1}");
        out.println("@attribute DANN numeric");
        out.println("");
        out.println("@data");
        try(BufferedReader br = Files.newBufferedReader(Paths.get(dataRoot + fileName))) {
            String line = br.readLine(); // reading the header
            boolean cs = true;
            while((line = br.readLine()) != null) {
                String[] items = line.split("\t", -1);
                if (!(items[32].equals("1/1") || items[32].equals("0/1"))) {
                    continue;
                }
                for(int i = 0; i < items.length; i++) {
                    if (items[i].equals(".")) items[i] = "?";
                }
                out.print(items[31]);
                for(int i = 0; i < 31; i++) {
                    out.print("," + items[i]);
                }
                out.print("," + items[33]); // CADD
                out.print("," + items[34]); // GWAVA
                out.print("," + items[36]); // SIM
                out.print("," + items[32]); // geno
                out.print("," + items[35]); // DANN
                out.println();
            }
            br.close();
        }
        out.close();
    }

    public String sortResults(String fileName) throws Exception {
        String resDataRoot = TEST_DATA_ROOT + "model4_without_dbnsfp/";
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
        String dataRoot = TEST_DATA_ROOT;
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
            new FileWriter(dataRoot + "model4_without_dbnsfp.res"), 104857600));
        for (String res: fileNames) {
            out.println(res);
        }
        out.close();

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
}
