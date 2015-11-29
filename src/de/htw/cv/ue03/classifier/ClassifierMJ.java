package de.htw.cv.ue03.classifier;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import de.htw.ba.facedetection.ImagePatternClassifier;
import de.htw.ba.facedetection.IntegralImage;
import de.htw.ba.facedetection.TestImage;

/**
 * 
 * @author Marie Manderla, Philipp Jährling
 * @date 28.11.2015
 *
 */
public class ClassifierMJ implements ImagePatternClassifier {

	private Rectangle area;
	private List<Rectangle> plusAreas;
	private List<Rectangle> minusAreas;
	private double weight;
	
	/**
	 * Create a weak classifier and set it's boundaries.
	 * 
	 * @param maxWidth
	 * @param maxHeight
	 */
	public ClassifierMJ(int maxWidth, int maxHeight) {
		this.area = new Rectangle(0, 0, maxWidth, maxHeight);
		this.plusAreas = new ArrayList<Rectangle>();
		this.minusAreas = new ArrayList<Rectangle>();
	}
	
	/**
	 * Create a weak classifier by passing plus and minus areas and a classifier weight.
	 * 
	 * @param plusAreas
	 * @param minusAreas
	 * @param weight
	 */
	public ClassifierMJ(ArrayList<Rectangle> plusAreas, ArrayList<Rectangle> minusAreas, double weight) {	
		this.weight = weight;
		this.plusAreas = new ArrayList<Rectangle>();
		this.minusAreas = new ArrayList<Rectangle>();
		this.area = new Rectangle(0, 0, 0, 0);
		
		for (Rectangle rec : plusAreas) {
			this.area.add(rec); // resize the classifier rectangle		
			this.addPlusPattern(rec); // add the area
		}
		for (Rectangle rec : minusAreas) {
			this.area.add(rec); // resize the classifier rectangle	
			this.addMinusPattern(rec); // add the area
		}
	}

	@Override
	public ImagePatternClassifier getScaledInstance(double scale) {	
		ClassifierMJ scaled = new ClassifierMJ((int)(area.width * scale), (int)(area.height * scale));
		
		scaled.weight = this.weight;
		
		for (Rectangle rec : plusAreas) {
			scaled.addPlusPattern(scaleRec(rec, scale));
		}
		for (Rectangle rec : minusAreas) {
			scaled.addMinusPattern(scaleRec(rec, scale));
		}
		
		return scaled;
	}
	
	/**
	 * Create a scaled Rectangle from a given one, multiplied by the given scale factor
	 * 
	 * @param rec
	 * @param scale
	 * @return
	 */
	private Rectangle scaleRec(Rectangle rec, double scale) {
		return new Rectangle(
			(int)(rec.x * scale), 		// x
			(int)(rec.y * scale), 		// y
			(int)(rec.width * scale), 	// width
			(int)(rec.height * scale)	// height
		);
	}

	/**
	 * Add a plus (light area) pattern rectangle to the classifier
	 * 
	 * @param rec
	 */
	public void addPlusPattern(Rectangle rec) {
		if ( isInBoundaries(rec, area.width, area.height) ) {
			plusAreas.add(rec);
		}
	}
	
	/**
	 * Add a minus (dark area) pattern rectangle to the classifier
	 * 
	 * @param rec
	 */
	public void addMinusPattern(Rectangle rec) {
		if ( isInBoundaries(rec, area.width, area.height) ) {
			minusAreas.add(rec);
		}
	}
	
	/**
	 * Check if a given rectangle is inside of another ones boundaries.
	 * 
	 * @param rec
	 * @param maxWidth
	 * @param maxHeight
	 * @return
	 */
	private boolean isInBoundaries(Rectangle rec, int maxWidth, int maxHeight) {
		double maxX = rec.getX() + rec.getWidth();
		double maxY = rec.getY() + rec.getHeight();
		return (maxX <= maxWidth && maxY <= maxHeight);
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
	
	/**
	 * Normalize a given value (used e.g. to normalize the correlation value)
	 * 
	 * @param num
	 * @param min
	 * @param max
	 * @return
	 */
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
		g2d.drawRect(x, y, area.width, area.height);
		
		Color plus = new Color(0x88FFFFFF, true);
		for (Rectangle r : plusAreas) {
			g2d.setColor(plus);
			g2d.fillRect(x + area.x + r.x, y + area.y + r.y, r.width, r.height);
		}
		
		Color minus = new Color(0x88000000, true);
		for (Rectangle r : minusAreas) {
			g2d.setColor(minus);
			g2d.fillRect(x + area.x + r.x, y + area.y + r.y, r.width, r.height);
		}
		
	}
}
