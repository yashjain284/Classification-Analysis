import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class HAC {
	public static HashMap<Integer, Integer> actualClusters = new HashMap<>();
	public static HashMap<Integer, Integer> resultClusters = new HashMap<>();
	public static HashMap<Integer, ArrayList<Double>> geneAttributes = new HashMap<>();
	public static TreeMap<String, TreeMap<String, Double>> distanceMatrix = new TreeMap<>();
	public static HashMap<String, String> minMatrix = new HashMap<>();
	public static ArrayList<String> allClusters = new ArrayList<>();
	public static List<double[][]> pca_list = new ArrayList<double[][]>();
	public static int totalGenes = 0;
	public static int reqNoOfClusters;
	public static int totalCluster = 0;
	public static int cols = 0;
	
	/**
	 * Takes filename and number of clusters as input from user.
	 * And while total clusters is not equal to required number of clusters, 
	 * we merge two 
	 * and finally it prints the clusters and jacard coeefficient for output set.
	 */
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter file name");
		String file = sc.nextLine();
		fileRead(file);
		System.out.println("Enter required number of clusters");
		reqNoOfClusters =Integer.parseInt(sc.nextLine());
		makeInitialDistanceMatrix();
		while(totalCluster != reqNoOfClusters) {
			mergeNext();
		}
		printClusters();
		double jac = getJaccard();
		System.out.println("Jaccard coefficient = "+jac);
//		double rand = getRand();
//		System.out.println("rand = "+rand);
	}
	
	/**
	 * Reads the file and stores the ground truth and feature attributes
	 * @param file input file
	 */
	private static void fileRead(String file) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = "";
			while((line=br.readLine())!=null) {
				String data[] = line.split("\\t");
				int id = Integer.parseInt(data[0]);
				int groundTruth =  Integer.parseInt(data[1]);
				actualClusters.put(id, groundTruth);
				ArrayList<Double> temp = new ArrayList<>();
				for(int i=2;i<data.length;i++) {
					temp.add(Double.parseDouble(data[i]));
				}
				cols = data.length-2;
				geneAttributes.put(id, temp);
				allClusters.add(id+"");
				totalGenes++;
			}
			totalCluster = totalGenes;
			br.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create a matrix for each cluster, and plots the output using 
	 * imported PCA implementation
	 */
	public static void printClusters() {
		int count = 0;
		double[][] matrix = null;
		for(String s:allClusters) {
			count++;
			System.out.println(s);
			String points[] = s.split(",");
			matrix = new double[points.length][cols];
			int k=0;
			for(String p:points) {
				resultClusters.put(Integer.parseInt(p), count);
				ArrayList<Double> features = geneAttributes.get(Integer.parseInt(p));
				double[] arr = new double[cols];
				for(int i=0;i<features.size();i++)
					arr[i] = features.get(i);
				matrix[k++] = arr;
			}
			pca_list.add(matrix);
		}
		Plot plot = new Plot((ArrayList<double[][]>) pca_list, "HAC");
		plot.plot();
	}
	
	/**
	 * Creates the initial distance matrix by calculating euclidean matrix between each points
	 */
	private static void makeInitialDistanceMatrix() {
		try {
			for(int i=1;i<=totalGenes;i++) {
				TreeMap<String, Double> temp1 = new TreeMap<>();
				if(distanceMatrix.containsKey(i+""))
					temp1 = distanceMatrix.get(i+"");
				for(int j=i+1;j<=totalGenes;j++) {
					double dist = getDist(i, j);
					TreeMap<String, Double> temp2 = new TreeMap<>();
					if(distanceMatrix.containsKey(j+""))
						temp2 = distanceMatrix.get(j+"");
					temp2.put(i+"", dist);
					distanceMatrix.put(j+"", temp2);
					temp1.put(j+"", dist);
					updateMinMatrix(i+"",j+"",dist);
				}
				distanceMatrix.put(i+"", temp1);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * find the minimum distance from the matrix and merges those two clusters and 
	 * updates corresponding distance by taking the minimum distance between two points from either clusters
	 * update the number of clusters.
	 */
	private static void mergeNext() {
		String ids[] = getMinGeneIds();
		String minI = ids[0];
		String minJ = ids[1];
		String newEntry = createNewEntry(minI, minJ);
		TreeMap<String, Double> prevI = distanceMatrix.remove(minI);
		TreeMap<String, Double> prevJ = distanceMatrix.remove(minJ);
		minMatrix.remove(minI);minMatrix.remove(minJ);
		allClusters.remove(minI);allClusters.remove(minJ);
		TreeMap<String, Double> newTemp = new TreeMap<String, Double>(); 
		for(String c: allClusters) {
			TreeMap<String, Double> temp = distanceMatrix.get(c);
			temp.remove(minI);temp.remove(minJ);
			double dist1 = prevI.get(c);
			double dist2 = prevJ.get(c);
			if(dist1 < dist2) {
				newTemp.put(c,dist1);
				updateMinMatrix(newEntry, c, dist1);
				temp.put(newEntry, dist1);
				String minData[] = minMatrix.get(c).split(";");
				if(minData[0].equals(minI) || minData[0].equals(minJ)) {
					minMatrix.remove(c);
					createMinMatrixAfterRemoval(c);
				}
				updateMinMatrix(c, newEntry, dist1);
			} else {
				newTemp.put(c,dist2);
				updateMinMatrix(newEntry, c, dist2);
				temp.put(newEntry, dist2);
				String minData[] = minMatrix.get(c).split(";");
				if(minData[0].equals(minI) || minData[0].equals(minJ)) {
					minMatrix.remove(c);
					createMinMatrixAfterRemoval(c);
				}
				updateMinMatrix(c, newEntry, dist2);
			}
			distanceMatrix.put(c, temp);
		}
		distanceMatrix.put(newEntry, newTemp);
		allClusters.add(newEntry);
		totalCluster = allClusters.size();
	}
	
	/**
	 * if the removed cluster was in min distance for other clusters, we remove that entry
	 * @param c cluster
	 */
	private static void createMinMatrixAfterRemoval(String c) {
		TreeMap<String, Double> temp = distanceMatrix.get(c);
		for(String s:allClusters) {
			if(!s.equals(c)) {
				double dist = temp.get(s);
				updateMinMatrix(c,s,dist);
			}
		}
	}

	/**
	 * it check for current minimum distance for cluster i and j. If given distance is 
	 * less than current stored distance, it updates. 
	 * @param i cluster i
	 * @param j cluster j
	 * @param dist distance between cluster i and cluster j
	 */
	private static void updateMinMatrix(String i, String j, double dist) {
		if(minMatrix.containsKey(i)) {
			String minData[] = minMatrix.get(i).split(";");
			double prevDist = Double.parseDouble(minData[1]);
			if(dist < prevDist)
				minMatrix.put(i+"", j+";"+dist);
		} else {
			minMatrix.put(i+"", j+";"+dist);
		}
		if(minMatrix.containsKey(j)) {
			String minData[] = minMatrix.get(j).split(";");
			double prevDist = Double.parseDouble(minData[1]);
			if(dist < prevDist)
				minMatrix.put(j, i+";"+dist);
		} else {
			minMatrix.put(j, i+";"+dist);
		}
	}

	/**
	 * creates a new cluster from two given clusters in lexicographical order
	 * @param minI cluster I
	 * @param minJ cluster J
	 * @return returns new cluster
	 */
	private static String createNewEntry(String minI, String minJ) {
		String first[] = minI.split(",");
		String second[] = minJ.split(",");
		ArrayList<Integer> arr = new ArrayList<>();
		for(int i=0;i<first.length;i++)
			arr.add(Integer.parseInt(first[i]));
		for(int j=0;j<second.length;j++)
			arr.add(Integer.parseInt(second[j]));
		Collections.sort(arr);
		String result = arr.get(0)+"";
		for(int i=1;i<arr.size();i++) {
			result = result + "," + arr.get(i);
		}
		return result;
	}

	/**
	 * finds minimum from minmatrix where key is each cluster and value is min distance to any cluster 
	 * @return array of two clusters
	 */
	private static String[] getMinGeneIds() {
		String []output = new String[2];
		String minI = "", minJ = "";
		double min = Double.MAX_VALUE;
		for (Map.Entry<String, String> entry : minMatrix.entrySet()) {
			String minData[] = entry.getValue().split(";");
			double currDist = Double.parseDouble(minData[1]);
			if(currDist < min) {
				minI = entry.getKey();
				minJ = minData[0];
				min = currDist;
			}
		}
		output[0] = minI;output[1] = minJ;
		return output;
	}
	
	/**
	 * Calculates the euclidean distance between two points
	 * @param id1 point 1
	 * @param id2 point 2
	 * @return Euclidean distance between point 1 and point 2
	 */
	public static double getDist(int id1, int id2) {
		ArrayList<Double> gA1 = geneAttributes.get(id1);
		ArrayList<Double> gA2 = geneAttributes.get(id2);
		double dist = 0;
		for(int i=0;i<gA1.size();i++) {
			dist += ( (gA1.get(i) - gA2.get(i)) * (gA1.get(i) - gA2.get(i)) );
		}
		dist = Math.sqrt(dist);
		return dist;
	}
	
	/*
	 * Calculates and returns the external index(Jacard) using the given ground truth and 
	 * resultant clusterc obtained above. 
	 */
	public static double getJaccard() {
		double match = 0;
		double unmatch = 0;
		for(int i = 1;i<=totalGenes;i++) {
			int c1 = actualClusters.get(i);
			int r1 = resultClusters.get(i);
			for(int j=1;j<=totalGenes;j++) {
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
	
	/**
	 * it returns the rand index for the dataset
	 * @return rand index
	 */
	/*public static double getRand() {
		double match11 = 0;
		double match00 = 0;
		double unmatch = 0;
		for(int i = 1;i<=totalGenes;i++) {
			int c1 = actualClusters.get(i);
			int r1 = resultClusters.get(i);
			for(int j=1;j<=totalGenes;j++) {
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
		double jacard = (match11+match00)/(match11+ match00+unmatch);
		return jacard;
	}*/
}
