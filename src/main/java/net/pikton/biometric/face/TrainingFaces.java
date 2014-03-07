package net.pikton.biometric.face;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author K&R Warno
 * 
 */
public class TrainingFaces {

	List<Face> faces;
	
	List<Face> substractedFaces;
	
	Face avgFace;
	
	final int faceLength;

	
	public TrainingFaces(int aFaceLength) {
		faceLength = aFaceLength;
		faces = new ArrayList<Face>();
		substractedFaces = new ArrayList<Face>();
	}

	public void addFace(Face aFace) {
		faces.add(aFace);
		substractedFaces = calculateSubstractedFaces(faces, true);
	}

	public void removeFace(Face aFace) {
		faces.remove(aFace);
		this.substractedFaces = calculateSubstractedFaces(faces, true);
	}

	public List<Face> getFaces() {
		return faces;
	}
	
	public List<Face> getSubstractedTrainingFaces(){
		return substractedFaces;
	}
	
	/**
	 * Calculates given set of faces with substracted avg face of training set
	 * @return
	 */
	public List<Face> calculateSubstractedFaces(List<Face> aFacesList){
		return calculateSubstractedFaces(aFacesList, false);
	}
	
	/**
	 * 
	 * @param aFacesList
	 * @param recalculateAvgFace
	 * @return
	 */
	List<Face> calculateSubstractedFaces(List<Face> aFacesList, boolean recalculateAvgFace){
		Face avgFace = getAvgFace(recalculateAvgFace);
		List<Face> substractedFaces = new ArrayList<Face>();
		for (Face f :aFacesList){
			substractedFaces.add(f.deepCopy());
		}

		for (int i = 0; i < substractedFaces.size(); i++) {
			for (int j = 0; j < faceLength; j++){
				substractedFaces.get(i).getFaceVector()[j]-=avgFace.getFaceVector()[j];
			}
		}
		return substractedFaces;
	}	
	
	public Face getAvgFace(boolean recalculateAvgFace){
		if (recalculateAvgFace){
			calculateAvgFace();
		}
		return this.avgFace;
	}
	
	void calculateAvgFace(){
		double[] zero = new double[faceLength];
		Arrays.fill(zero, 0d);
		Face avgF = new Face("avg", zero);
		double[] fv = avgF.getFaceVector();
		for (int i = 0; i < faceLength; i++){
			for (int j = 0; j < faces.size(); j++) {
				fv[i]+=faces.get(j).getFaceVector()[i];
			}
			fv[i]/=faces.size()*1d;
		}
		this.avgFace = avgF;
	}
}