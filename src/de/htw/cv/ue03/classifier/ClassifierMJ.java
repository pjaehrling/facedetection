package de.htw.cv.ue03.classifier;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import de.htw.ba.facedetection.ImagePatternClassifier;
import de.htw.ba.facedetection.IntegralImage;
import de.htw.ba.facedetection.TestImage;

public class ClassifierMJ implements ImagePatternClassifier {

	private Rectangle area;
	private List<Rectangle> plusAreas;
	private List<Rectangle> minusAreas;
	private double weight;
	
	public ClassifierMJ(double maxWidth, double maxHeight) {
		this.area = new Rectangle(0, 0, (int)maxWidth, (int)maxHeight);
		plusAreas = new ArrayList<Rectangle>();
		minusAreas = new ArrayList<Rectangle>();
	}

	@Override
	public ImagePatternClassifier getScaledInstance(double scale) {	
		ClassifierMJ scaled = new ClassifierMJ(area.width * scale, area.height * scale);
		
		scaled.weight = this.weight;
		
		for (Rectangle rec : plusAreas) {
			scaled.addPlusPattern(scaleRec(rec, scale));
		}
		for (Rectangle rec : minusAreas) {
			scaled.addMinusPattern(scaleRec(rec, scale));
		}
		
		return scaled;
	}
	
	private Rectangle scaleRec(Rectangle rec, double scale) {
		return new Rectangle((int)(rec.x * scale), (int)(rec.y * scale), 
				(int)(rec.width * scale), (int)(rec.height * scale));
	}

	public void addPlusPattern(Rectangle rec) {
		plusAreas.add(rec);
	}
	
	public void addMinusPattern(Rectangle rec) {
		minusAreas.add(rec);
	}
	
	@Override
	public double matchAt(TestImage image, int posX, int posY) {
		IntegralImage integral = image.getIntegralImage();
		int plusMean = 0;
		int minusMean = 0;
		
		for (Rectangle r : plusAreas) {
			plusMean += integral.meanValue(r.x + posX, r.y + posY, r.width, r.height);
		}
		
		for (Rectangle r : minusAreas) {
			minusMean += integral.meanValue(r.x + posX, r.y + posY, r.width, r.height);
		}
		
		double correlation = Math.abs(plusMean - minusMean);
		correlation = normalize(correlation, 0, 255);
		return correlation;
	}

	@Override
	public double matchAt(TestImage image, int posX, int posY, double threshold) {
		double match = matchAt(image, posX, posY);
		return match > threshold ?  match : 0;
	}
	
	private double normalize(double num, double min, double max) {
		return (num - min) / (max - min);
	}
	
	@Override
	public Rectangle getArea() {
		return area;
	}

	@Override
	public double getWeight() {
		return weight;
	}

	@Override
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	@Override
	public String toString() {
		return "MyClassifier [area=" + area + ", weight=" + weight + "]";
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
