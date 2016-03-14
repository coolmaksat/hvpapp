import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.file.*;

import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.Instance;


public class Classification {

    RandomForest cls;

    public Classification(Properties props) throws Exception {
        // Loading the saved classifier
        String rfModelFile = props.getProperty("randomForestModelFile");
        this.cls = (RandomForest)weka.core.SerializationHelper.read(rfModelFile);

    }

    public boolean[] classify(String fileName) throws Exception {
        FileReader fr = new FileReader(fileName);
        Instances is = new Instances(fr);
        is.setClassIndex(0);
        Result[] results = new Result[is.numInstances()];
        for (int i = 0; i < is.numInstances(); i++) {
            Instance instance = is.instance(i);
            // System.out.println(instance);
            double[] result = this.cls.distributionForInstance(instance);
            results[i] = new Result(i, result[0]);
            // System.out.print(instance.value(0) + " ");
            // for(int j = 0; j < result.length; j++) {
            //     System.out.print(result[j] + " ");
            // }
            // System.out.println();
        }

        Arrays.sort(results, Collections.reverseOrder());
        boolean[] ret = new boolean[2];
        ret[0] = results[0].i == 0 && results[0].d > 0.5;
        for (int i = 0; i < results.length; i++) {
            if (results[i].i == 0 && results[i].d > 0.5) {
                ret[1] = true;
                break;
            }
        }
        System.out.println(Arrays.toString(ret));
        return ret;
    }

    public void classifyAll() throws Exception {
        String dataRoot = "data/models/set1/";
        DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(dataRoot), "*.arff");
        int n = 0, c = 0, j = 0;
        for (Path filePath: files) {
            String fileName = filePath.toString();
            boolean[] res = new boolean[2];
            try {
                res = this.classify(fileName);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(fileName);
            }
            if (res[0]) c++;
            if (res[1]) j++;
            n++;
        }

        System.out.println(n + " " + c + " " + j);

    }

    public void toArffAll() throws Exception {
        String dataRoot = "data/models/set1/";
        DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(dataRoot), "*.out");
        for (Path filePath: files) {
            String fileName = filePath.getFileName().toString();
            if (!Files.exists(Paths.get(filePath.toString() + ".arff"))) {
                this.toArff(fileName);
            }
        }

    }
    public void toArff(String fileName) throws Exception {
        String dataRoot = "data/models/set1/";
        PrintWriter out = new PrintWriter(new BufferedWriter(
            new FileWriter(dataRoot + fileName + ".arff"), 1073741824));
        out.println("@relation " + fileName);
        out.println();
        out.println("@attribute TYPE {CASE,CTRL}");
        out.println("@attribute SIFT_score numeric");
        out.println("@attribute Polyphen2_HDIV_score numeric");
        out.println("@attribute LRT_score numeric");
        out.println("@attribute MutationTaster_score numeric");
        out.println("@attribute MutationAssessor_score numeric");
        out.println("@attribute FATHMM_score numeric");
        out.println("@attribute PROVEAN_score numeric");
        out.println("@attribute DANN_score numeric");
        out.println("@attribute MetaSVM_score numeric");
        out.println("@attribute GERP_NR numeric");
        out.println("@attribute GERP_RS numeric");
        out.println("@attribute CADD numeric");
        out.println("@attribute GWAVA numeric");
        out.println("@attribute SIM numeric");
        out.println("");
        out.println("@data");
        out.println("");
        try(BufferedReader br = Files.newBufferedReader(Paths.get(dataRoot + fileName))) {
            String line;
            boolean cs = true;
            while((line = br.readLine()) != null) {
                String[] items = line.split("\t", -1);
                if (cs) {
                    out.print("CASE");
                    cs = false;
                } else {
                    out.print("CTRL");
                }
                for(int i = 3; i < items.length; i++) {
                    if (items[i].equals(".")) items[i] = "?";
                    out.print("," + items[i]);
                }
                out.println();
            }
            br.close();
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
