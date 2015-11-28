package de.htw.cv.ue03.classifier;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import de.htw.ba.facedetection.ImagePatternClassifier;
import de.htw.ba.facedetection.TestImage;

public class StrongClassifierMJ implements ImagePatternClassifier {

	private List<ClassifierMJ> weakClassifiers;

	
	public StrongClassifierMJ() {
		weakClassifiers = new ArrayList<ClassifierMJ>();
	}

	public void addWeakClassifier(ClassifierMJ classifier) {
		this.weakClassifiers.add(classifier);
	}
	
	@Override
	public ImagePatternClassifier getScaledInstance(double scale) {
		StrongClassifierMJ scaled = new StrongClassifierMJ();
		
		for (ClassifierMJ classifier : weakClassifiers) {
			scaled.addWeakClassifier((ClassifierMJ) classifier.getScaledInstance(scale));
		}
		
		return scaled;
	}

	@Override
	public double matchAt(TestImage image, int posX, int posY) {
		double match = 0;
		
		for (ClassifierMJ classifier : weakClassifiers) {
			match += classifier.matchAt(image, posX, posY) * classifier.getWeight();
		}
		
		return match;
	}

	@Override
	public double matchAt(TestImage image, int posX, int posY, double threshold) {
		double match = 0;
		
		for (ClassifierMJ classifier : weakClassifiers) {
			match += classifier.matchAt(image, posX, posY, threshold) * classifier.getWeight();
		}
		
		return match;
	}

	@Override
	public Rectangle getArea() {
		Rectangle area = new Rectangle(0, 0, 0, 0);
		
		for (ClassifierMJ classifier : weakClassifiers) {
			area.add(classifier.getArea());
		}
		
		return area;
	}

	@Override
	public double getWeight() {
		return 1;
	}

	@Override
	public void setWeight(double weight) {
	}

	@Override
	public void drawAt(Graphics2D g2d, int x, int y) {
		g2d.setColor(Color.GREEN);
		
		Rectangle area = getArea();
		int posX = x + (int)area.getX();
		int posY = y + (int)area.getY();
		int w = (int)area.getWidth();
		int h = (int)area.getHeight();

    	g2d.drawRect(posX, posY, w, h);
	}

}
