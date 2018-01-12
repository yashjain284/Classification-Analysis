/*This is a part of existing implementation of PCA used to reduce dimension for plotting the result.
 * Reference: https://github.com/CSE601-DataMining/Clustering/blob/master/src/edu/buffalo/cse/clustering/PCA.java
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jmat.data.AbstractMatrix;
import org.jmat.data.Matrix;
import org.jmat.data.matrixDecompositions.EigenvalueDecomposition;

public class PCA {

	private AbstractMatrix covariance;
	private AbstractMatrix EigenVectors;
	private AbstractMatrix EigenValues;
	private double[][] rawData;

	public PCA(double[][] rawData) {
		this.rawData = rawData;
	}

	public AbstractMatrix getVectors() {
		return EigenVectors;
	}

	public AbstractMatrix getValues() {
		return EigenValues;
	}

	public AbstractMatrix getReducedMatrix(int redDim) {
		AbstractMatrix X1 = new org.jmat.data.Matrix(rawData);
		covariance = X1.covariance();
		EigenvalueDecomposition e = covariance.eig();
		EigenVectors = e.getV();
		EigenValues = e.getD();

		// make list of all eigen values
		AbstractMatrix ev = this.getValues().max();
		List<EigenVal> evList = new ArrayList<EigenVal>();
		for (int i = 0; i < ev.getColumnDimension(); i++) {
			EigenVal eval = new EigenVal(i, ev.get(0, i));
			evList.add(eval);
		}

		Collections.sort(evList);

		AbstractMatrix finalMat = getReducedMatrix(redDim, this.getVectors(), evList, X1);
		return finalMat;

	}

	public AbstractMatrix getReducedMatrix(int dim, AbstractMatrix eVectors, List<EigenVal> evList,
			AbstractMatrix data) {
		AbstractMatrix finalMat = null;
		for (int i = 0; i < dim; i++) {
			AbstractMatrix eVec = eVectors.getRow(evList.get(i).getIndex());
			double[][] newCol = new double[data.getRowDimension()][1];
			for (int j = 0; j < data.getRowDimension(); j++) {
				AbstractMatrix dataRow = data.getRow(j).transpose();
				newCol[j][0] = eVec.times(dataRow).get(0, 0);
			}
			if (finalMat == null) {
				finalMat = new Matrix(newCol);
			} else {
				AbstractMatrix newAbsCol = new Matrix(newCol);
				finalMat = finalMat.mergeColumns(newAbsCol);
			}
		}

		return finalMat;
	}

}

class EigenVal implements Comparable<EigenVal> {
	int index;

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public double getVal() {
		return val;
	}

	public void setVal(double val) {
		this.val = val;
	}

	double val;

	EigenVal(int index, double val) {
		this.index = index;
		this.val = val;
	}

	@Override
	public int compareTo(EigenVal o) {
		double oVal = ((EigenVal) o).getVal();
		if (oVal > this.val)
			return 1;
		else if (oVal < this.val)
			return -1;
		else
			return 0;
	}

	@Override
	public String toString() {
		return index + ":" + this.val;
	}
}