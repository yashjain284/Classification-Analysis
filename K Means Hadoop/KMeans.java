import java.io.*;
import java.util.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.*;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class KMeans{

  public static int noOfIterations = 0;//To count the no of iterations.
  public static boolean isConverged = false; //Boolean flag for the convergence condition of K means.
  public static int noOfClusters = 0; //Number of clusters.
  public static double jaccardMax = 0.0; 
  public static double randMax = 0.0;
  public static HashMap<Integer, Integer> actualClusters = new HashMap<Integer, Integer>();//Map to store ground truth
  public static HashMap<Integer, Integer> resultClusters = new HashMap<Integer, Integer>();//Map to store the actual result
  //Map to store all the genes (Key: RowID, Value: List of attributes)
  public static HashMap<Integer, ArrayList<Double>> genePool = new HashMap<Integer, ArrayList<Double>>();
  //List which stores the centroidList which is currently being used in the Map-Reduce phase.
  public static ArrayList<ArrayList<Double>> centroidList = new ArrayList<ArrayList<Double>>();
  public static ArrayList<Double> jaccardList = new ArrayList<Double>();
  public static ArrayList<Double> randList = new ArrayList<Double>();
  //List to store the final clusters in form of a string. (Used to print it to the FinalOutput.txt file.)
  public static ArrayList<String> finalCluster = new ArrayList<String>();

  //Function to create compute distance between a centroid and a data point.
  public static double calculateDistance(ArrayList<Double> centroid, int dataPointIndex){

    ArrayList<Double> dataPoint = genePool.get(dataPointIndex);

    double distance = 0.0;
    for(int i=0;i<dataPoint.size();i++) {
      distance += ( (dataPoint.get(i) - centroid.get(i)) * (dataPoint.get(i) - centroid.get(i)) );
    }
    distance = Math.sqrt(distance);
    return distance;
  }
  
  /**
    * This class represents the Mapper Phase of the K Mean algorithm
    * Input format : Key :File(Object), Value :Line(Text)
    * 
    * Map Phase :
    * Each line of the file is read in value.
    * For the given data point we give the closest centroid by calculating the distance with all the centroids.
    * 
    * Output format : Key: Centroid(String) Value: RowID of Gene(IntWritable)
    *   
    */
  public static class KMeansMapper
  extends Mapper<Object, Text, Text, IntWritable>{

    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();

    /**
    * Function to set up the centroid list initally. If it's the first step, we pick initalize cetroids
    * Otherwise the centroidList will contain the newly computed centroids from the last iteration.
    */
    protected void setup(Context context) throws IOException, InterruptedException {
      Configuration conf = context.getConfiguration();
      if(centroidList.size() == 0)
        centroidList = pickRandomCentroids(noOfClusters);
      else
        System.out.println("Centroid List already initalized");
    }

    //It performs the map task of the K means algorithm.
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
      StringTokenizer itr = new StringTokenizer(value.toString());

      String line = value.toString();
      String strip[] = line.split("\t");
      //Getting the rowID
      int row = Integer.parseInt(strip[0]);

      int cIndex = 0;
      //Loop to calculate the closest centroid.
      Double minDistance = Double.MAX_VALUE;
      for(int i=0;i<centroidList.size();i++){

        Double dist = calculateDistance(centroidList.get(i),row);
        if(dist < minDistance){
          minDistance = dist;
          cIndex = i;
        }
      }

      //Converting the centroid as a Text Value (KEY)
      String ctr = centroidList.get(cIndex).toString();
      Text centroid = new Text(ctr);
      //Data point is passed in the form of a RowID (IntWritable).
      IntWritable dataPoint = new IntWritable(row);
      context.write(centroid,dataPoint);
    }

    /**
    *This function will pick n initial clusters randomly. (Note : We have currently preset the values for optimum results.)
    * Returns the list of centroid points.
    */
    public ArrayList<ArrayList<Double>> pickRandomCentroids(int n){

      Set<Integer> picked = new HashSet<Integer>();
      Random rand = new Random();  

      while (picked.size() < n) {
        picked.add(1 + rand.nextInt(genePool.size()));
      }

      ArrayList<ArrayList<Double>> clist = new ArrayList<ArrayList<Double>>();
      for(int temp: picked)
        clist.add(genePool.get(temp));
      
      return clist;
    }
  }

  /**
    * This class represents the Mapper Phase of the K Mean algorithm
    * Input format : Key :Centroid(Text), Value :List of DataPoints(Iterable-IntWritables)
    * 
    * Reduce Phase :
    *
    * For every centroid point we take the list of the data points in the cluster and compute the new centroid for it.
    * We also update the centroidList with the update centroid.
    * Output format : Key: Cluster Number (Text) Value: List of RowID (Text)
    *   
    */
  public static class KMeansReducer extends Reducer<Text,IntWritable,Text,Text> {

    //Create a new centroid list to newly computed centroids.
    ArrayList<ArrayList<Double>> newCentroidList = new ArrayList<ArrayList<Double>>();
    //Intialize the cluster number to 1.
    int clusterNo = 1;
    //Function implementing the reduce phase of the K Means.
    public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {

      int ctr = 0;
      int dpsize = genePool.get(1).size();

      //Create a empty list to store
      ArrayList<Double> newCentroid = new ArrayList<Double>(Collections.nCopies(dpsize, 0.0));
      String res = "";
      //Loop to iterate all the data points in the list and keep adding it to the new computed centroid.
      for(IntWritable val: values){
        resultClusters.put(val.get(),clusterNo);
        res = res +" "+val.get();
        ArrayList<Double> dataPoint = genePool.get(val.get());
        for(int i=0;i<dataPoint.size();i++)
          newCentroid.set(i, (newCentroid.get(i) + dataPoint.get(i)));
        ctr++;
      }
      //Computing average for the new centroid
      for (int i=0;i<newCentroid.size() ;i++ ){
        newCentroid.set(i, newCentroid.get(i)/ctr);
      }
      //Adding the computed centroid to a the new centroid list.
      newCentroidList.add(newCentroid);
      //Adding it to the final cluster list.
      finalCluster.add("Cluster "+(clusterNo++)+" : "+res);
      //Writing the cluster to the output file.
      context.write(new Text("Cluster "+(clusterNo)+" : \t"), new Text(res));
    }

    /**
    * This function runs after the reduce task has completed and we have a new set of centroids.
    * We check for convergence condition for K means here, by comparing the newCentroidList with the old centroidList.
    * If the lists are same, we set the isConverged to true and the process stops here.
    */
    protected void cleanup(Context context) throws IOException, InterruptedException {

      Configuration conf = context.getConfiguration();
      //Loop to compare both the list of centroid.
      for(int i=0;i<centroidList.size(); i++){
        String str1 = centroidList.get(i).toString();
        String str2 = newCentroidList.get(i).toString();
        if(str1.equals(str2))
          isConverged = true;
        else{
          isConverged = false;
          break;
        } 
      }
      
      //If it converged then we clear the cluster output and replace the old centroid the newly computed centroid list.
      if(!isConverged){
        centroidList.clear();
        finalCluster.clear();
        centroidList = newCentroidList;

      }
      else{
        System.out.println("***********************************************************");
        System.out.println("K MEANS HAS CONVERGED");
        System.out.println("***********************************************************");
      }

      //Computing RAND INDEX and JACCARD INDEX for the new clusters.
      double currRand = getRand(); 
      randList.add(currRand);
      double currJaccard = getJaccard(); 
      jaccardList.add(currJaccard);
      
      System.out.println("*******************************************");
      System.out.println(" Jaccard Coeffecient    : "+currJaccard);
      //System.out.println(" Rand Coeffecient       : "+currRand);
      System.out.println("*******************************************");
      randMax = (currRand > randMax)?currRand:randMax;
      jaccardMax = (currJaccard > jaccardMax)?currJaccard:jaccardMax;

    }
  }//Reducer class ends here.


  public static void printOutput() throws IOException{

    FileSystem fs = FileSystem.get(new Configuration());
    Path file = new Path("./ClusterOutput.txt");
    //Delete if te existing output file exists.
    if ( fs.exists( file ))
      fs.delete(file,true);  
    OutputStream os = fs.create(file);
    BufferedWriter br = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));

    for (Map.Entry<Integer , Integer> entry : resultClusters.entrySet()) {
      int dp = entry.getKey();
      int cluster = entry.getValue();
      String coordinate = genePool.get(dp).toString();
      coordinate = coordinate.substring(1,coordinate.length()-1);
      coordinate = coordinate.replace(",","");
      coordinate = coordinate.replace(" ","\t");
      //System.out.println(coordinate +"\t"+cluster);
      br.write(coordinate+"\t"+cluster+"\n");
    }
    br.close();
  }

  //Function to read the file initally to populate the genePool map and the ground trut clusters.
  public static void readInputFile(String path)throws IOException{
    Path filePath = new Path(path);
    FileSystem fs = FileSystem.get(new Configuration());
    BufferedReader br=new BufferedReader(new InputStreamReader(fs.open(filePath)));
    String line;
    while ((line=br.readLine()) != null){
      String data[] = line.split("\\t");
      int id = Integer.parseInt(data[0]);
      
      int groundTruth =  Integer.parseInt(data[1]); 
      actualClusters.put(id, groundTruth); //Adding the ground truth value.

      ArrayList<Double> temp = new ArrayList<Double>();
      for(int i=2;i<data.length;i++) {
        temp.add(Double.parseDouble(data[i]));
      }
      genePool.put(id, temp); //Adding the gene data to the gene Pool Map.
    }
  }

  //Function to compute Jaccard INDEX
  public static double getJaccard() {
    double match = 0;
    double unmatch = 0;
    for(int i = 1;i<=genePool.size();i++) {
      int c1 = actualClusters.get(i);
      int r1 = resultClusters.get(i);
      for(int j=1;j<=genePool.size();j++) {
        int c2 = actualClusters.get(j);
        int r2 = resultClusters.get(j);
        if(c1 == c2 && r1 == r2)
          match++;
        else if( (c1 == c2 && r1 != r2) || (c1 != c2 && r1 == r2))
          unmatch++;
      }
    }
    double jacard = match/(match+unmatch);
    return jacard;
  }

  //Function to compute Rand INDEX
  public static double getRand() {
    double match11 = 0;
    double match00 = 0;
    double unmatch = 0;
    for(int i = 1;i<=genePool.size();i++) {
      int c1 = actualClusters.get(i);
      int r1 = resultClusters.get(i);
      for(int j=1;j<=genePool.size();j++) {
        int c2 = actualClusters.get(j);
        int r2 = resultClusters.get(j);
        if(c1 == c2 && r1 == r2)
          match11++;
        else if( (c1 == c2 && r1 != r2) || (c1 != c2 && r1 == r2))
          unmatch++;
        else
          match00++;
      }
    }
    double rand = (match11+match00)/(match11+ match00+unmatch);
    return rand;
  }

  //Driver function
  public static void main(String[] args) throws Exception {
    //Start logging time.
    long startTime = System.currentTimeMillis();

    if(args[0].contains("cho.txt"))
      noOfClusters = 5;
    else if(args[0].contains("iyer.txt"))
      noOfClusters = 10;
    else 
      noOfClusters = 3;

    FileSystem fs = FileSystem.get(new Configuration());
    Configuration conf = new Configuration();
    readInputFile(args[0]);
    //Loop the Map-Reduce process till the centroid converge.
    while(isConverged==false){
      fs.delete(new Path("./output"), true); //Delete the output file of the same name if it exists.

      //Configuring hadoop Job.
      Job job = Job.getInstance(conf, "k means");
      job.setJarByClass(KMeans.class);
      job.setMapperClass(KMeansMapper.class);
      job.setReducerClass(KMeansReducer.class);
      job.setNumReduceTasks(1);
      job.setOutputKeyClass(Text.class);
      job.setOutputValueClass(IntWritable.class);
      FileInputFormat.addInputPath(job, new Path(args[0]));
      FileOutputFormat.setOutputPath(job, new Path("./output"));
      job.waitForCompletion(true);
      noOfIterations++;
    }
    long endTime = System.currentTimeMillis();
    //End logging time.
    printOutput();
    System.out.println("Time taken : "+(endTime-startTime) + " ms"); 
    Path file = new Path("./FinalOutput.txt");
    //Delete if te existing output file exists.
    if ( fs.exists( file ))
      fs.delete(file,true);  
    OutputStream os = fs.create(file);
    BufferedWriter br = new BufferedWriter(new OutputStreamWriter(os,"UTF-8"));

    //Writing all clusters
    for(int i=0;i<finalCluster.size();i++){

      br.write("Centroid "+(i+1)+" : "+centroidList.get(i).toString()+"\n");
      br.write(finalCluster.get(i)+"\n\n");
    }

    br.write("\n\nTime taken : "+(endTime-startTime) + " ms\n");
    //Writing Number of iterations.
    br.write("Total Number of Iterations : "+noOfIterations+"\n\n");
    //Writing Max Jaccard.
    
    //Writing All Rand and Jaccard Iterations.
    // for(int i = 0;i<noOfIterations;i++)
    //   br.write("Iteration "+(i+1)+" : "+" jaccard : "+jaccardList.get(i)/*" rand : "+jaccardList.get(i)+*/+"\n");
    
    //Writing Max Jaccard.
    br.write("Max Jaccard   : "+jaccardMax+"\n");
    br.write("Final Jaccard : "+jaccardList.get(noOfIterations-1)+"\n");

    System.out.println("Total Number of Iterations :  "+noOfIterations);
    System.out.println("End of task");
    br.close();
    fs.close();
  }
}