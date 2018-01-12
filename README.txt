RUNNING K MEANS

1. Run python file (k_means.py) and provide 4 following input:
- path: path to file
- num_of_cluster: number of clusters needed
- initial_point_of_clusters: space seperated gene_ids for initial centroids
- num_of_iterations: number of iterations needed

Example:
python k_means.py
Enter file path: cho.txt
Enter number of clusters: 5
Enter ids of points to initialize as centroids: 1 2 3 4 5
Enter number of iterations: 25


RUNNING HIERARCHICAL AGGLOMERATIVE CLUSTERING WITH SINGLE LINK(MIN)

1. Include all JFree jars( provided in folder as External jars dependency)

2. Run HAC.java using any IDE or using command line (command => "javac HAC.java" to compile and "java HAC.java" to run)

3. System prompts for file path(with file name) followed by number of clusters required.

4. System outputs clusters formed, followed by jacard, and result visualization.



RUNNING DENSITY BASED CLUSTERING(DBSCAN)

1. Include all JFree jars( provided in folder as External jars dependency)

2. Run DBScan.java using any IDE or using command line (command => "javac DBScan.java" to compile and "java DBScan.java" to run)

3. System prompts for file path(with file name) followed by epsilon radius and min points.

4. System outputs no of clusters formed, followed by jacard, and result visualization.



RUNNING K MEANS ON HADOOP

1. Store you KMeans.java file and your input files in a directory of your choice.
2. Change the present working directory to that folder
cd <Directory_Name>

3. Start Hadoop on the VM/Single Node cluster.
hadoop-start.sh

4 create a directory on hdfs called input
hdfs dfs -mkdir input

5. We have to copy our input/dataset files to our HDFS input folder.
hdfs dfs -put ./cho.txt ./input
hdfs dfs -put ./iyer.txt ./input
hdfs dfs -put ./new_dataset_1.txt ./input

6. Compile the KMeans.java file with command.
hadoop com.sun.tools.javac.Main KMeans*.java

7. Create a runnable jar from the class file generated.
jar cf KMeans.jar ./Kmeans*.class

8. Run the Hadoop job on the (Jar).
hadoop jar KMeans.jar KMeans ./input/iyer.txt
hadoop jar KMeans.jar KMeans ./input/cho.txt
hadoop jar KMeans.jar KMeans ./input/new_dataset_1.txt

9. Move the output Files genrated to the host machine.
hdfs dfs -get ./FinalOutput.txt ./
hdfs dfs -get ./ClusterOutput.txt ./

10. Open the files on the host machine to view the results

11. Visualize clusters using PCA on python script.
python3 pca_and_plot.py ClusterOutput.txt






