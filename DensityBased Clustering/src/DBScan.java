import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;

public class DBScan {
	public static HashMap<Integer, Integer> actualClusters = new HashMap<>();
	public static HashMap<Integer, Integer> resultClusters = new HashMap<>();
	public static HashMap<Integer, HashSet<Integer>> clustersVsPoints = new HashMap<>();
	public static HashMap<Integer, ArrayList<Double>> geneAttributes = new HashMap<>();
	public static List<double[][]> pca_list = new ArrayList<double[][]>();
	public static int cols = 0;
	public static int totalGenes = 0;
	public static int clusters = 0;
	
	/**
	 * It takes filename, epsilon radius and minimum points as input and
	 * prints jacard coefficient and plots the graph
	 */
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter file name");
		String file = sc.nextLine();
		fileRead(file);
		System.out.println("Enter Epsilon radius");
		double eps = Float.parseFloat(sc.nextLine());
		System.out.println("minimum points");
		int minpts = Integer.parseInt(sc.nextLine());
		boolean visited[] = new boolean[totalGenes+1];
		DBScanAlgo(eps, minpts, visited);
		System.out.println("No of clusters formed = "+clusters);
		double jacard = getJaccard();
		System.out.println("Jacard coeff = "+jacard);
		//double rand = getRand();
		//System.out.println("Rand = "+rand);
		printClusters();
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
				totalGenes++;
			}
			br.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * for each points, creates the clusters for each and puts all neighbours in epsilon radius to the same clusters
	 * @param eps Epsilon radius
	 * @param minpts minimum points
	 * @param visited visited boolean matrix for each points
	 */
	private static void DBScanAlgo(double eps, int minpts, boolean[] visited) {
		for(int id=1;id<=totalGenes;id++) {
			if(visited[id] == false) {
				visited[id] = true;
				Queue<Integer> neighbours = getNeighbours(id, eps);
				if(neighbours.size() < minpts) {
					resultClusters.put(id, -1);
				} else {
					clusters++;
					expandClusters(id, neighbours, eps, minpts, visited);
				}
			}
		}
	}

	/**
	 * Expands the current cluster by adding neighbours of neighbours at epsilon radius 
	 * @param id point i
	 * @param neighbours Existing list of neighbours
	 * @param eps Epsilon radius
	 * @param minpts minimum points in neighbourhood
	 * @param visited boolean matrix for each points
	 */
	private static void expandClusters(int id, Queue<Integer> neighbours, double eps, int minpts, boolean []visited) {
		resultClusters.put(id, clusters);
		while(!neighbours.isEmpty()) {
			int n = neighbours.poll();
			if(visited[n] == false) {
				visited[n] = true;
				Queue<Integer> neighboursNext = getNeighbours(n, eps);
				if(neighboursNext.size() >= minpts) {
					neighbours.addAll(neighboursNext);
				}
			}
			if(!resultClusters.containsKey(n) || resultClusters.get(n) == -1)
				resultClusters.put(n, clusters);
		}
	}

	/**
	 * Find the neighbour for point i at a epsilon radius from point i 
	 * @param id point i
	 * @param eps Epsilon radius
	 * @return Collection of all neighbours that are at a epsilon distance from given point
	 */
	private static Queue<Integer> getNeighbours(int id, double eps) {
		Queue<Integer> result = new LinkedList<>();
		for(int i=1;i<=totalGenes;i++) {
			if( getDist(id, i) <= eps)
				result.add(i);
		}
		return result;
	}

	/**
	 * Calculates the euclidean distance between two points
	 * @param id1 point 1
	 * @param id2 point 2
	 * @return Euclidean distance between point 1 and point 2
	 */
	private static double getDist(int id1, int id2) {
		ArrayList<Double> gA1 = geneAttributes.get(id1);
		ArrayList<Double> gA2 = geneAttributes.get(id2);
		double dist = 0;
		for(int i=0;i<gA1.size();i++) {
			dist += ( (gA1.get(i) - gA2.get(i)) * (gA1.get(i) - gA2.get(i)) );
		}
		dist = Math.sqrt(dist);
		return dist;
	}
	
	/**
	 * Calculates and returns the external index(Jacard) using the given ground truth and 
	 * resultant clusterc obtained above. 
	 */
	private static double getJaccard() {
		double match = 0;
		double unmatch = 0;
		for(int i = 1;i<=totalGenes;i++) {
			int c1 = actualClusters.get(i);
			int r1 = resultClusters.get(i);
			HashSet<Integer> temp = new HashSet<>();
			if(clustersVsPoints.containsKey(r1))
				temp = clustersVsPoints.get(r1);
			temp.add(i);
			clustersVsPoints.put(r1, temp);
			for(int j=1;j<=totalGenes;j++) {
				int c2 = actualClusters.get(j);
				int r2 = resultClusters.get(j);
				HashSet<Integer> temp2 = new HashSet<>();
				if(clustersVsPoints.containsKey(r2))
					temp2 = clustersVsPoints.get(r2);
				temp2.add(j);
				clustersVsPoints.put(r2, temp2);
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
	 * Create a matrix for each cluster, and plots the output using 
	 * imported PCA implementation
	 */
	public static void printClusters() {
		double[][] matrix = null;
		for(Map.Entry<Integer, HashSet<Integer>> entry : clustersVsPoints.entrySet()) {
			matrix = new double[entry.getValue().size()][cols];
			int k=0;
			System.out.println("Cluster no = "+entry.getKey());
			HashSet<Integer>temp = entry.getValue();
			for(Integer t:temp) {
				System.out.print(t+" ");
				ArrayList<Double> features = geneAttributes.get(t);
				double[] arr = new double[cols];
				for(int i=0;i<features.size();i++)
					arr[i] = features.get(i);
				matrix[k++] = arr;
			}
			System.out.println();
			pca_list.add(matrix);
		}
		//using existing implementation to plot the output
		Plot plot = new Plot((ArrayList<double[][]>) pca_list, "DBSCAN");
		plot.plot();
	}
	
	/**
	 * it returns the rand index for the dataset
	 * @return rand index
	 */
	/*private static double getRand() {
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
		double rand = (match11+match00)/(match11+ match00+unmatch);
		return rand;
	}*/
}
