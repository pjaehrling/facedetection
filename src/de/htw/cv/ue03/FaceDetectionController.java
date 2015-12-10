/**
 * @author Nico Hezel
 */
package de.htw.cv.ue03;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import de.htw.ba.facedetection.ImagePatternClassifier;
import de.htw.ba.facedetection.IntegralImage;
import de.htw.ba.facedetection.TestImage;
import de.htw.cv.ue03.classifier.ClassifierMJ;
import de.htw.cv.ue03.classifier.DiagonalClassifierMJ;
import de.htw.cv.ue03.classifier.EdgeHorizontalClassifierMJ;
import de.htw.cv.ue03.classifier.EdgeVerticalClassifierMJ;
import de.htw.cv.ue03.classifier.LineHorizontalClassifierMJ;
import de.htw.cv.ue03.classifier.LineVerticalClassifierMJ;
import de.htw.cv.ue03.classifier.StrongClassifierMJ;

public class FaceDetectionController {
	
	private enum Method { Graustufen, IntegralBild, ViolaJones };

	private static final int WHITE = 0xFF000000 | (255<<16) | (255<<8) | 255;
	private static final int BLACK = 0xFF000000 | (0<<16) | (0<<8) | 0;
	private static final int NEIGHBOUR = 5;
	
	@FXML
	private ImageView leftImageView;
	
	@FXML
	private ImageView rightImageView;
	
	@FXML
	private ComboBox<Method> methodSelection;
	
	@FXML
	private CheckBox showMatchesChb;
	
	@FXML
	private Label runtimeLabel;
		
	@FXML
	private Label thresholdValue;
		
	@FXML
	private Slider thresholdSlider;
	
	private int[] cleanSrc;
	private TestImage image;
		
	@FXML
	public void initialize() {
		methodSelection.getItems().addAll(Method.values());
		methodSelection.setValue(Method.Graustufen);
		methodSelection.setOnAction(this::runMethod);
		
		loadImage(new File("gesicht.jpg"));
		
		thresholdSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
			public void changed(ObservableValue<? extends Number> ov, Number old_val, Number new_val) {
            	thresholdValue.setText(String.format("%.2f", new_val));
            	runMethod(new ActionEvent());
            }
        });
	}

	@FXML
	public void onOpenFileClick() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(new File(".")); 
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Images (*.jpg, *.png, *.gif)", "*.jpeg", "*.jpg", "*.png", "*.gif"));
		loadImage(fileChooser.showOpenDialog(null));
	}
	
	public void loadImage(File file) {		
		if(file != null) {
			leftImageView.setImage(new Image(file.toURI().toString()));
			Image leftImage = leftImageView.getImage();
			cleanSrc = imageToPixel(leftImage);
			
		   	IntegralImage ii = new IntegralImageMJ(cleanSrc, (int)leftImage.getWidth(), (int)leftImage.getHeight());
			image = TestImage.createJolieTestImage(ii);
			// image = new TestImage(ii);
			runMethod(null);
		}		
	}
	
	public int[] imageToPixel(Image image) {
		int width = (int)image.getWidth();
		int height = (int)image.getHeight();
		int[] pixels = new int[width * height];
		image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
		return pixels;
	}
	
	public int[] imageToPixel(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		int[] pixels = new int[width * height];
		image.getRGB(0, 0, width, height, pixels, 0, width);
		return pixels;
	}
	
	public Image pixelToImage(int[] pixels, int width, int height) {
		WritableImage wr = new WritableImage(width, height);
		PixelWriter pw = wr.getPixelWriter();
		pw.setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
		return wr;
	}
	
	@FXML
	public void runMethod(ActionEvent event) {

		// no images loaded
		if(leftImageView.getImage() == null)
			return;
		
	  	// get image dimensions
    	int srcWidth = (int)leftImageView.getImage().getWidth();
    	int srcHeight = (int)leftImageView.getImage().getHeight();
    	int dstWidth = srcWidth;
    	int dstHeight = srcHeight;
    	
    	// get pixels arrays
    	int srcPixels[] = Arrays.copyOf(cleanSrc, cleanSrc.length);
    	int dstPixels[] = new int[dstWidth * dstHeight];
    	
    	double threshold = thresholdSlider.getValue();
    	
		long startTime = System.currentTimeMillis();

		// get method choice 
		Method currentMethod = methodSelection.getSelectionModel().getSelectedItem();
		switch (currentMethod) {
			case Graustufen:
				doGray(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight);
				break;
			case IntegralBild:	// visualize the integral image
				image.getIntegralImage().toIntARGB(dstPixels);
	    		break;
	    	case ViolaJones:	// run a full face detection
	    		doVoilaJones(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight, threshold);
	    		break;
		}
		
		rightImageView.setImage(pixelToImage(dstPixels, dstWidth, dstHeight));
		leftImageView.setImage(pixelToImage(srcPixels, srcWidth, srcHeight));
    	runtimeLabel.setText("Methode " + currentMethod + " ausgeführt in " + (System.currentTimeMillis() - startTime) + " ms");
	}
    
    private void doGray(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight) {
		// loop over all pixels of the destination image
		for (int y = 0; y < dstHeight; y++) {
			for (int x = 0; x < dstWidth; x++) {
				int pos	= y * dstWidth + x;
				
				int c = srcPixels[y * srcWidth + x]; // RGB Value
				int r = (c>>16)&0xFF;
				int g = (c>> 8)&0xFF;
				int b = (c    )&0xFF;
					
				int lum = (int) (0.299*r + 0.587*g + 0.114*b); // luminance
				lum = Math.min(lum,255);
				dstPixels[pos] = 0xFF000000 | (lum<<16) | (lum<<8) | lum;
			}
		}
    }
    
	/** 
	 * Erstelle einige Weak Classifier und versuchte das Gesicht im Bild damit zu erkennen.
	 * 
	 * @param srcPixels
	 * @param srcWidth
	 * @param srcHeight
	 * @param dstPixels
	 * @param dstWidth
	 * @param dstHeight
	 * @param threshold
	 * @param scale
	 */
    private void doVoilaJones(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], int dstWidth, int dstHeight, double threshold) 
    {    	
    	/*
    	// First two classifiers: eye (right/left) = forehead (light), eye area (dark), cheek (light)
    	ArrayList<Rectangle> plus = new ArrayList<Rectangle>();
    	ArrayList<Rectangle> minus = new ArrayList<Rectangle>();
    	plus.add( new Rectangle(0, 0, 1, 1) ); // forehead - top
    	minus.add( new Rectangle(0, 1, 1, 1) ); // eye area - middle
    	plus.add( new Rectangle(0, 2, 1, 1) ); // cheek - bottom
    	
    	ImagePatternClassifier eyeLeft = new ClassifierMJ(0, 0, plus, minus, 0.3, 0);
    	ImagePatternClassifier eyeRight = new ClassifierMJ(2, 0, plus, minus, 0.3, 0);
    	eyeLeft = eyeLeft.getScaledInstance(48);
    	eyeRight = eyeRight.getScaledInstance(48);
    	
    	
    	// Second classifier: nose - left/right (dark) and inner (light)
    	plus = new ArrayList<Rectangle>();
    	minus = new ArrayList<Rectangle>();
    	minus.add( new Rectangle(0, 0, 10, 60) ); // outer nose - left
    	plus.add( new Rectangle(10, 0, 20, 60) ); // inner nose - middle
    	minus.add( new Rectangle(30, 0, 10, 60) ); // outer nose - right   	
    	ImagePatternClassifier nose = new ClassifierMJ(50, 60, plus, minus, 0.1, 0);
    	
    	// Third classifier: nose end - top (dark) and bottom (light)
    	plus = new ArrayList<Rectangle>();
    	minus = new ArrayList<Rectangle>();
    	minus.add( new Rectangle(0, 0, 40, 15) ); // end nose - top
    	plus.add( new Rectangle(0, 15, 40, 15) ); // under nose - bottom
    	ImagePatternClassifier noseEnd = new ClassifierMJ(50, 128, plus, minus, 0.2, 0);
 
    	
    	// Third classifier: mouth - top (dark) and chin - bottom (light)
    	plus = new ArrayList<Rectangle>();
    	minus = new ArrayList<Rectangle>();
    	minus.add( new Rectangle(0, 0, 60, 30) ); // mouth - top
    	plus.add( new Rectangle(0, 30, 60, 30) ); // chin - bottom	
    	ImagePatternClassifier mouth = new ClassifierMJ(45, 160, plus, minus, 0.1, 0);
    	
    	// Create weak classifier list and add classifiers
    	ArrayList<ImagePatternClassifier> weakClassifiers = new ArrayList<ImagePatternClassifier>();
    	weakClassifiers.add(eyeLeft);
    	weakClassifiers.add(eyeRight);
    	weakClassifiers.add(nose);
    	weakClassifiers.add(noseEnd);
    	weakClassifiers.add(mouth);
    	*/
    	
    	ArrayList<ImagePatternClassifier> weakClassifiers = new ArrayList<ImagePatternClassifier>();
    	weakClassifiers.add(new LineVerticalClassifierMJ(10, 0, 0.2, 0.0).getScaledInstance(20));
    	weakClassifiers.add(new LineHorizontalClassifierMJ(0, 10, 0.2, 0.0).getScaledInstance(20));
    	weakClassifiers.add(new DiagonalClassifierMJ(3, 3, 0.2, 0.0).getScaledInstance(20));
    	weakClassifiers.add(new EdgeHorizontalClassifierMJ(0, 5, 0.2, 0.0).getScaledInstance(20));
    	weakClassifiers.add(new EdgeVerticalClassifierMJ(5, 0, 0.2, 0.0).getScaledInstance(20));
  
    	// create strong classifier out of weak ones
    	StrongClassifierMJ face = new StrongClassifierMJ(weakClassifiers);
     	
     	// wie groß ist der Klassifier
		Rectangle area = face.getArea();

		
		// durchlaufe das Bild, ignoriere die Ränder
     	for (int y = 0; y < srcHeight-area.getHeight()*0.8; y++) {	
			for (int x = 0; x < srcWidth-area.getWidth()*0.8; x++)	{
				int pos = y * srcWidth + x;
				
				// berechne den Korrelationswert an jeder Position
				double correlation = face.matchAt(image, x, y, threshold);
				
				// zeichne das Korrelationsbild 
				int grey = (int)(correlation * 255.0);
				dstPixels[pos] =  (0xFF << 24) | (grey << 16) | (grey << 8) | grey;	
			}
		}
     	
     	// erstelle eine Kopie vom Eingangsbild
		BufferedImage bufferedImage = new BufferedImage(srcWidth, srcHeight, BufferedImage.TYPE_INT_ARGB);
    	bufferedImage.setRGB(0, 0, srcWidth, srcHeight, srcPixels, 0, srcWidth);
    	Graphics2D g2d = bufferedImage.createGraphics();
    	
    	// Get maxims and draw strong classifier at max positions
    	int[] maximas = new int[dstHeight * dstWidth];
     	getCorrelationMaximas(dstPixels, maximas, dstWidth, dstHeight, 0.95);
    	
    	for (int y = 0; y < dstHeight; y++) {	
			for (int x = 0; x < dstWidth; x++)	{
				int pos = y * srcWidth + x;
				
				if (maximas[pos] == WHITE) {
					face.drawAt(g2d, x, y);
					
					// TODO ... hacky break after one maxima was found
					y = dstHeight;
					x = dstWidth;
				}
					
			}
		}
     	
     	// schreibe die Kopie in die Eingangspixel zurück
    	g2d.dispose();
		bufferedImage.getRGB(0, 0, srcWidth, srcHeight, srcPixels, 0, srcWidth);  
    }
    
    /**
     * 
     * @param srcPixels
     * @param dstPixels
     * @param width
     * @param height
     * @param threshold
     */
    private void getCorrelationMaximas(int srcPixels[], int dstPixels[], int width, int height, double threshold) {
     	int min = getCorrelationMin(srcPixels);
    	int max = getCorrelationMax(srcPixels);
    	if (max - min == 0) {
    		return;
    	}
    	
    	for (int y = 0; y < height; y++) {	
			for (int x = 0; x < width; x++)	{
				int pos = y * width + x;
				
				int grey = (srcPixels[pos] >> 16) & 0xFF;
				double greyNorm = normalize(grey, min, max);
    			
	    		if (greyNorm > threshold && biggerThanNeighbours(srcPixels, x, y, width, height)) {
	    			dstPixels[pos] = WHITE;
	    			System.out.println("WHITE --> " + grey);
	    		} else {
	    			dstPixels[pos] = BLACK;
	    		}
			}
    	}	
    }
    
    /**
     * 
     * @param array
     * @param x
     * @param y
     * @param width
     * @param height
     * @return
     */
	private boolean biggerThanNeighbours(int array[], int x, int y, int width, int height)
	{
		int pos = y * width + x;
		int mid = NEIGHBOUR / 2;
		for (int j = 0; j < NEIGHBOUR; j++) {
			for (int i = 0; i < NEIGHBOUR; i++) {
				int neighbourX = i - mid;
				int neighbourY = j - mid;
				int posComp = (y + neighbourY) * width + (x + neighbourX);
				if (isInImage(x + neighbourX, y + neighbourY, width, height)) {
					if (array[posComp] > array[pos]) {
						return false;
					}
				}
			}
		}
		return true;
	}
	
	/**
	 * 
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @return
	 */
	private boolean isInImage(int  x, int y, int width, int height) {
		return  x < width && 
				x > -1 &&	
				y < height &&
				y > -1;
	}
    
	/**
	 * 
	 * @param srcPixels
	 * @return
	 */
    private int getCorrelationMin(int srcPixels[]) {
    	int min = 255;
    	
    	for (int i = 0; i < srcPixels.length; i++) {
    		int corr = (srcPixels[i]>>16)&0xFF;
    		if (corr < min) {
    			min = corr;
    		}
    	}
  
    	return min;
    }
    
    /**
     * 
     * @param srcPixels
     * @return
     */
    private int getCorrelationMax(int srcPixels[]) {
    	int max = 0;
    	
    	for (int i = 0; i < srcPixels.length; i++) {
    		int corr = (srcPixels[i]>>16)&0xFF;
    		if (corr > max) {
    			max = corr;
    		}
    	}
    	
    	return max;
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
}
