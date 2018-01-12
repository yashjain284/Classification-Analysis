import os, sys, math, pca_and_plot

def read_file(path, num_of_cluster):
	'''
	This function accepts input file path and number of clusters,
	and return data (gene_id to gene_data dictionary), 
	cluster_to_point_list (cluster to list of gene_ids dictionary) and 
	actual_cluster (gene_id to cluster dictionary)
	'''
	
	actual_cluster = {}
	cluster_to_point_list ={}
	data = {}
	
	cluster_count = num_of_cluster
	
	with open(path, "r") as file:
		for line in file:
			line_split = line.strip().split("\t")
			g_id = int(line_split[0])
			actual_cluster[g_id] = int(line_split[1])
			data[g_id] = [float(d) for d in line_split[2:]]
	
	record_count = len(actual_cluster)
	cr_count = math.ceil(record_count/cluster_count)
	
	for i in range(1, record_count+1):
		cl = math.ceil(i/cr_count)
		if cl not in cluster_to_point_list:
			cluster_to_point_list[cl] = []
		cluster_to_point_list[cl] = cluster_to_point_list[cl] + [i]
	
	return data, cluster_to_point_list, actual_cluster


def calculate_centroid(data, point_list):
	'''
	This function accepts data (gene_id to gene_data dictionary) and point_list (list of gene_ids),  
	and return centroid of points
	'''
	
	centroid = [0.0] * len(data[1])
	
	for point in point_list:
		for i, coord in enumerate(data[point]):
			centroid[i] = centroid[i] + coord
	for i in range(len(centroid)):
		centroid[i] = centroid[i]/len(point_list)
	return centroid


def calculate_centroids(data, cluster_to_point_list):
	'''
	This function accepts data (gene_id to gene_data dictionary) and cluster_to_point_list (cluster to list of gene_ids dictionary),  
	and return centroids of clusters (cluster to centroid dictionary)
	'''
	
	centroids = {}
	for cluster, point_list in cluster_to_point_list.items():
		centroids[cluster] = calculate_centroid(data, point_list)
	return centroids


def find_distance(point, centroid):
	'''
	This function accepts point and centroid,  
	and return euclidean distance
	'''
	
	dist = 0
	for x, y in zip(point, centroid):
		dist = dist + (x-y)*(x-y)
	return math.sqrt(dist)


def find_closest_cluster(point, centroids):
	'''
	This function accepts point and centroids (cluster to centroid dictionary),  
	and return cluster id of nearest centroid to point by comparing euclidean distance
	'''
	
	closest_cluster = 1
	min_dist = find_distance(point, centroids[1])
	for cluster, centroid in centroids.items():
		curr_dist = find_distance(point, centroid)
		if curr_dist < min_dist:
			min_dist = curr_dist
			closest_cluster = cluster
	return closest_cluster


def change_clusters(data, cluster_to_point_list, centroids):
	'''
	This function accepts data (gene_id to gene_data dictionary), cluster_to_point_list (cluster to list of gene_ids dictionary) 
	and centroids (cluster to centroid dictionary),  
	and return new_cluster_to_point_list (cluster to list of gene_ids dictionary) by assigning point to cluster with nearest centroid
	'''
	
	changed = False
	new_cluster_to_point_list = {}
	
	for cluster, point_list in cluster_to_point_list.items():
		for point in point_list:
			new_cluster = find_closest_cluster(data[point], centroids)
			if new_cluster not in new_cluster_to_point_list:
				new_cluster_to_point_list[new_cluster] = []
			new_cluster_to_point_list[new_cluster] = new_cluster_to_point_list[new_cluster] + [point]
			if(new_cluster != cluster):
				changed = True
	
	return changed, new_cluster_to_point_list


def find_point_to_cluster(cluster_to_point_list):
	'''
	This function accepts cluster_to_point_list (cluster to list of gene_ids dictionary) 
	and return point_to_cluster (gene_id to cluster id dictionary)
	'''
	
	point_to_cluster = {}
	for cluster, point_list in cluster_to_point_list.items():
		for point in point_list:
			point_to_cluster[point] = cluster
	return point_to_cluster


def find_jaccard(point_to_cluster, actual_cluster):
	'''
	This function accepts point_to_cluster (gene_id to cluster id dictionary) and actual_cluster (gene_id to cluster id dictionary)
	and return value of jaccard coefficent
	'''
	
	total_points = len(actual_cluster)
	match11 = 0.0
	match00 = 0.0
	unmatch = 0.0
	for i in range(1, total_points+1):
		
		c1 = actual_cluster[i]
		r1 = point_to_cluster[i]
		
		for j in range(1, total_points+1):
			c2 = actual_cluster[j]
			r2 = point_to_cluster[j]
			if(c1 == c2 and r1 == r2):
				match11 = match11 + 1
			elif( (c1 == c2 and r1 != r2) or (c1 != c2 and r1 == r2)):
				unmatch = unmatch + 1
			else:
				match00 = match00 + 1
		
	rand = (match11 + match00)/(match11 + match00 + unmatch)
	jaccard = (match11)/(match11 + unmatch)
	return rand, jaccard


def print_to_file_and_plot(data, cluster_to_point_list):
	'''
	This function accepts data (gene_id to gene_data dictionary) and cluster_to_point_list (cluster to list of gene_ids dictionary)
	and prints to txt file for pca and plot of points.
	'''
	path = "kmeans_result.txt"
	fo = open(path, "w")
	for cluster, point_list in cluster_to_point_list.items():
		for point in point_list:
			fo.write("\t".join(map(str, data[point])) + "\t" + str(cluster) + "\n")
	fo.close()
	pca_and_plot.pca_and_plot(path)


# Accepting input parameters
path = input("Enter file path: ")
num_of_cluster = int(input("Enter number of clusters: "))
initial_point_of_clusters = input("Enter ids of points to initialize as centroids: ")
initial_point_of_clusters = [int(p) for p in initial_point_of_clusters.split(" ")]
num_of_iterations = int(input("Enter number of iterations: "))


# Extracting data from file
data, cluster_to_point_list, actual_cluster = read_file(path, num_of_cluster)

# Assigning initial centroids
centroids = {}
for i, p in enumerate(initial_point_of_clusters):
	centroids[i+1] = data[p]
# Assigning points to clusters with nearest initial centroids
changed, cluster_to_point_list = change_clusters(data, cluster_to_point_list, centroids)

iterations = 1
max_jaccard = 0
max_rand_index = 0

while(changed and iterations<=num_of_iterations):
	# calculating centroids of current clusters
	centroids = calculate_centroids(data, cluster_to_point_list)
	# Assigning points to clusters with nearest centroids
	changed, cluster_to_point_list = change_clusters(data, cluster_to_point_list, centroids)
	iterations = iterations + 1
	
	# Calculating maximum rand-index and jaccard coefficient
	point_to_cluster = find_point_to_cluster(cluster_to_point_list)
	rand_index, jaccard = find_jaccard(point_to_cluster, actual_cluster)
	print(jaccard)
	max_rand_index, max_jaccard = max(max_rand_index, rand_index), max(max_jaccard, jaccard)

print("\nIteration completed:\t", iterations-1, "\n")
for cluster in sorted(cluster_to_point_list):
	cluster_to_point_list[cluster] = sorted(cluster_to_point_list[cluster])
	print(cluster, centroids[cluster])
	print(cluster, cluster_to_point_list[cluster], "\n")

# Calculating final rand-index and jaccard coefficient
point_to_cluster = find_point_to_cluster(cluster_to_point_list)
rand_index, jaccard = find_jaccard(point_to_cluster, actual_cluster)

# print("Max Rand-Index\t\t" + str(max_rand_index))
print("Max Jaccard co-efficient\t\t" + str(max_jaccard))

# print("Final Rand-Index\t\t" + str(rand_index))
print("Final Jaccard co-efficient\t" + str(jaccard))

print_to_file_and_plot(data, cluster_to_point_list)
