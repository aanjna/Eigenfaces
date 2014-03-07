package net.pikton.biometric.face;

public interface FaceEngine {

	public abstract void addTrainingFace(Face aFace);

	public abstract void removeTrainingFace(Face aFace);

	public abstract TrainingFaces getTrainingFaces();

	public abstract Face identifyFace(Face aFace);

}