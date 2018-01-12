'''
Used existing implementation (PA1) for PCA and Plotting points.
'''

import sys, os
import numpy as np
import matplotlib.pyplot as plt

def read_file(path):
	with open(path, "r") as file:
		lines = file.readlines()

	rownum = len(lines)
	colnum = len(lines[0].split("\t"))

	data = [[]] * (colnum-1)
	sumval = [0] * (colnum-1)
	mean = []
	labels = []

	for line in lines:
		line_split = line.strip().split("\t")
		for i, val in enumerate(line_split):
			if(i != colnum-1):
				data[i] = data[i] + [float(val)]
				sumval[i] = sumval[i] + float(val)
			else:
				labels = labels + [val]

	np_data = np.array(data)
	unique_labels = list(set(labels))

	for s in sumval:
		mean.append(s/rownum)
	adjusted_data = data[:]

	for i in range(colnum-1):
		for j in range(rownum):
			adjusted_data[i][j] = adjusted_data[i][j] - mean[i]
	np_adjusted_data = np.array(adjusted_data)
	return np_data, np_adjusted_data, labels, unique_labels


def pca(np_data, np_adjusted_data, labels, unique_labels, file_name):
	# Calculating covariance
	np_cov = np.dot(np_adjusted_data, np_adjusted_data.T) / (np_adjusted_data.T.shape[0]-1)

	# Getting top 2 eigen vectors
	eigval, eigvec = np.linalg.eig(np_cov)
	key = np.argsort(eigval)[::-1][:2]
	eigval_top, eigvec_top = eigval[key], eigvec[:, key]

	# Transforming original data
	transformed_data = np.dot(np_data.T, eigvec_top)
	transformed_data_transpose = transformed_data.T
	plot_data(transformed_data, labels, unique_labels, "PCA for " + file_name)

def plot_data(data, labels, unique_labels, title):

	label_dict_x = {}
	label_dict_y = {}
	for label in unique_labels:
		label_dict_x[label] = []
		label_dict_y[label] = []
	for label, pt in zip(labels, data):
		label_dict_x[label] = label_dict_x[label] + [pt[0]]
		label_dict_y[label] = label_dict_y[label] + [pt[1]]

	plt_list = []
	for i, label in enumerate(unique_labels):
		plt_list= [(plt.plot(label_dict_x[label], label_dict_y[label], 'go', color=plt.cm.jet(1.*i/(len(unique_labels)-1)), label=label))]

	# http://matplotlib.org/users/legend_guide.html#legend-location
	plt.title(title)
	plt.legend(bbox_to_anchor=(1.05, 1), loc=2, borderaxespad=0.)	
	plt.show()

def pca_and_plot(path):
	file_name = os.path.basename(path)

	np_data, np_adjusted_data, labels, unique_labels = read_file(path)
	pca(np_data, np_adjusted_data, labels, unique_labels, file_name)

if __name__ == "__main__":
	path = sys.argv[1]
	pca_and_plot(path)