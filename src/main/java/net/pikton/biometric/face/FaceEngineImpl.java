package net.pikton.biometric.face;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.EigenvalueDecomposition;

/**
 * Eigenfaces algorithm implementation. Based on parer: M. Turk, A. Pentland "Eigenfaces for Recognition" Journal of Cognitive Neurosience Vol.3, Number 1 - 1991 MIT 
 * @author K&R Warno
 *
 */
public  class FaceEngineImpl implements FaceEngine {

	TrainingFaces trainingFaces ;
	
	final int faceLength;

	final int eigenvectorsNumber;
	
	final double distanceTreshhold;
	
	DoubleMatrix2D eigenvectors;
	
	DoubleMatrix1D distance;

	final static Logger logger = LoggerFactory.getLogger(FaceEngineImpl.class);	
	
	public FaceEngineImpl(int aFaceLength, int aMaxNumberEigenvectors, double aDistTresh){
		trainingFaces = new TrainingFaces(aFaceLength);
	    faceLength = aFaceLength;
	    eigenvectorsNumber = aMaxNumberEigenvectors;
	    distanceTreshhold = aDistTresh;
	}

	/* (non-Javadoc)
	 * @see net.pikton.biometric.face.IFaceEngine#addTrainingFace(net.pikton.biometric.face.Face)
	 */
	public void addTrainingFace(Face aFace){
		trainingFaces.addFace(aFace);
		calculateEigenfaces();
		fillFacesCoeficients(trainingFaces.getSubstractedTrainingFaces());
	}
	
	/* (non-Javadoc)
	 * @see net.pikton.biometric.face.IFaceEngine#removeTrainingFace(net.pikton.biometric.face.Face)
	 */
	public void removeTrainingFace(Face aFace){
		trainingFaces.removeFace(aFace);
		calculateEigenfaces();
		fillFacesCoeficients(trainingFaces.getSubstractedTrainingFaces());
	}	
	
	/* (non-Javadoc)
	 * @see net.pikton.biometric.face.IFaceEngine#getTrainingFaces()
	 */
	public TrainingFaces getTrainingFaces() {
		return trainingFaces;
	}

	DoubleMatrix2D getEigenfaces(){
		return eigenvectors;
	}
	
	/**
	 * 
	 * @return eigenfaces
	 */
	void calculateEigenfaces(){
		DoubleMatrix2D smallTransformMatrix = calculateSmallMatrix(trainingFaces.getSubstractedTrainingFaces());
		EigenvalueDecomposition decomposition = new EigenvalueDecomposition(smallTransformMatrix);
		DoubleMatrix1D realEigevalues =  decomposition.getRealEigenvalues();
		DoubleMatrix1D imagEigevalues =  decomposition.getImagEigenvalues();
		DoubleMatrix2D smallEigenvectors = decomposition.getV();
		
		//select list of biggest eigenvalues modulus
		SortedMap<Double, DoubleMatrix1D> map = new TreeMap<Double, DoubleMatrix1D>(); 
		for (int i = 0; i < realEigevalues.size(); i++){
			double modulus = Math.pow(realEigevalues.toArray()[i],2d) + Math.pow(imagEigevalues.toArray()[i],2d);
			map.put(new Double(modulus), smallEigenvectors.viewColumn(i));
		}//remove smallest elements
		for (int i = 0; i < map.size() - eigenvectorsNumber; i++){
			map.remove(map.firstKey());
		}
		List<DoubleMatrix1D> selectedEigenvectors = new ArrayList<DoubleMatrix1D>(map.values());
		
		//calculate eigenfaces
		DoubleMatrix2D facesTransposed = facesToMatrix2D(trainingFaces.getSubstractedTrainingFaces());
		DoubleMatrix2D selectedEigenvectorsTransposed = collection1DToArray2D(selectedEigenvectors);  
		Algebra alg = new Algebra();
		DoubleMatrix2D eigenfacesTransposed = alg.mult(selectedEigenvectorsTransposed, facesTransposed);
		DoubleMatrix2D result = alg.transpose(eigenfacesTransposed);
		normArray(result);
		this.eigenvectors = result;
	}	
	
	
	
	/**
	 * 
	 * @param aFace
	 */
	void decomposeFace(Face aFace){
		List<Face> list = new ArrayList<Face>();
		list.add(aFace);
		List<Face> substracted = trainingFaces.calculateSubstractedFaces(list);
		fillFacesCoeficients(substracted);
		aFace = substracted.get(0);
	}
	
	/* (non-Javadoc)
	 * @see net.pikton.biometric.face.IFaceEngine#identifyFace(net.pikton.biometric.face.Face)
	 */
	public Face identifyFace(Face aFace){
		decomposeFace(aFace);
		double[] distanceArr = new double[eigenvectorsNumber];
		SortedMap<Double, Face> distanceMap = new TreeMap<Double, Face>();
		double[] inputCoef = aFace.getFaceCoeficients();
		for (int i = 0; i < trainingFaces.getFaces().size(); i++){
			double[] trainCoef = trainingFaces.getFaces().get(i).getFaceCoeficients();
			for (int j = 0 ; j < eigenvectorsNumber; j++){
				distanceArr[i]+=Math.pow(inputCoef[j] - trainCoef[j],2);
			}
			distanceMap.put(distanceArr[i], trainingFaces.getFaces().get(i));
		}
		logger.info("distance map ->" + distanceMap);
		distanceMap = distanceMap.headMap(distanceTreshhold);
		if (!distanceMap.isEmpty()){
			double bestKey= distanceMap.firstKey();
			Face bestResult = distanceMap.get(distanceMap.firstKey());
			logger.info("smallest distance ->" + bestKey + "; best face ->" + bestResult);
			return bestResult;
		}
		return null;
	}
	
	/**
	 * 
	 * @param aFaces a set of substracted faces
	 */
	void fillFacesCoeficients(List<Face> aFaces){
		Algebra alg = new Algebra();
		DoubleMatrix2D transposed = alg.transpose(eigenvectors);
		for (Face face : aFaces){
			DoubleMatrix1D faceCoeficients = alg.mult(transposed, new DenseDoubleMatrix1D(face.getFaceVector()));
			face.setFaceCoeficients(faceCoeficients.toArray());
		}
	}
		
	/**
	 * calclulates transform array C*C<T> for training faces
	 * @param aFaces
	 */	
	DoubleMatrix2D calculateSmallMatrix(List<Face> aFaces) {
		DoubleMatrix2D facesMatrixTransposed = facesToMatrix2D(aFaces);
		Algebra algebra = new Algebra();
		DoubleMatrix2D facesMatrix = algebra.transpose(facesMatrixTransposed);
		return algebra.mult(facesMatrixTransposed, facesMatrix);
	}
		
	/**
	 * 
	 * @param aFaces
	 * @return transposed training faces array
	 */
	DoubleMatrix2D facesToMatrix2D(List<Face> aFaces){
		double[][] m = new double[aFaces.size()][faceLength];
		for (int i = 0; i < aFaces.size(); i++){
			m[i] = aFaces.get(i).getFaceVector();
		}
		DoubleMatrix2D matrix2d = new DenseDoubleMatrix2D(aFaces.size(),faceLength);
		return matrix2d;
	}
	
	/**
	 * 
	 * @param aVectors - list of 1D vectors
	 * @return transposed 2d array from collection of vectors
	 */
	DoubleMatrix2D collection1DToArray2D(List<DoubleMatrix1D> aVectors){
		if (aVectors.size() == 0){
			return new DenseDoubleMatrix2D(0, 0);
		}
		double[][] array2d = new double[aVectors.size()][aVectors.get(0).size()];
		for (int i = 0; i < aVectors.size(); i++){
			array2d[i] = aVectors.get(i).toArray();
		}	
		return new DenseDoubleMatrix2D(array2d);
	}
	
	/**
	 * 
	 * @param aTransposedVectorArray
	 * @return list of 1D vectors
	 */
	List<DoubleMatrix1D> array2DTocollection1D(DoubleMatrix2D aTransposedVectorArray){
		double[][] input = aTransposedVectorArray.toArray();
		List<DoubleMatrix1D> result = new ArrayList<DoubleMatrix1D>();
		
		for (int i = 0; i < input.length; i++){
			result.add(new DenseDoubleMatrix1D(input[i]));
		}
		return result;
	}
	
	/**
	 * 
	 * @param anInputArray
	 */
	void normArray(DoubleMatrix2D anInputArray){
		for (int j = 0; j < anInputArray.columns(); j++){
			double norm = 0;
			for ( int i = 0; i < anInputArray.rows(); i++){
				norm+= anInputArray.get(i, j)*anInputArray.get(i, j);
			}
			norm = Math.sqrt(norm);
			for ( int i = 0; i < anInputArray.rows(); i++){
				anInputArray.set(i, j,anInputArray.get(i, j)/norm);
			}
		}
	}
}
