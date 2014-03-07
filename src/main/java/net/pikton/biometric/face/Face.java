package net.pikton.biometric.face;

import java.util.Arrays;

public class Face implements Cloneable{

	double[] faceVector;
	double[] faceCoeficients;
	final String faceId;
	
	public Face(String anID, double[] aFaceVector){
		faceId = anID;	
		this.faceVector = aFaceVector;
	}
		
	public double[] getFaceVector(){
		return this.faceVector;
	}
	
	public double[] getFaceCoeficients() {
		return faceCoeficients;
	}

	public void setFaceCoeficients(double[] faceCoeficients) {
		this.faceCoeficients = faceCoeficients;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((faceId == null) ? 0 : faceId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Face other = (Face) obj;
		if (faceId == null) {
			if (other.faceId != null)
				return false;
		} else if (!faceId.equals(other.faceId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Face [faceVector=" + Arrays.toString(faceVector)
				+ ", faceCoeficients=" + Arrays.toString(faceCoeficients)
				+ ", faceId=" + faceId + ", toString()=" + super.toString()
				+ "]";
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new Face(new String(this.faceId), Arrays.copyOf(this.faceVector, this.faceVector.length));
	}	
	
	public Face deepCopy(){
		try {
			Face f = (Face)this.clone();
			return f;
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}
