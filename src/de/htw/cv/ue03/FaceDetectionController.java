/**
 * @author Nico Hezel
 */
package de.htw.cv.ue03;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
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
import de.htw.ba.facedetection.IntegralImage;
import de.htw.ba.facedetection.RandomClassifier;
import de.htw.ba.facedetection.TestImage;
import de.htw.cv.ue03.classifier.ClassifierMJ;
import de.htw.cv.ue03.classifier.StrongClassifierMJ;

public class FaceDetectionController {
	
	private enum Method { Graustufen, IntegralBild, ViolaJones };
	
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
	    		doVoilaJones(srcPixels, srcWidth, srcHeight, dstPixels, threshold);
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
	 * TODO erstelle einige Weak Classifier und versuchte das Gesicht im Bild damit zu erkennen.
	 * 
	 * @param srcPixels
	 * @param srcWidth
	 * @param srcHeight
	 * @param dstPixels
	 * @param threshold
	 * @param scale
	 */
    private void doVoilaJones(int srcPixels[], int srcWidth, int srcHeight, int dstPixels[], double threshold) 
    {    	
    	int width = 1;
    	int height = 2;
     	ClassifierMJ horizontal = new ClassifierMJ(width, height);
		int halfHeight = (int) height / 2;
		Rectangle top = new Rectangle(0, 0, width, halfHeight);
		Rectangle bottom = new Rectangle(0, halfHeight, width, halfHeight);
		horizontal.addPlusPattern(bottom);
		horizontal.addMinusPattern(top);
		horizontal = (ClassifierMJ) horizontal.getScaledInstance(15);
		horizontal.setWeight(0.25);
		
		width = 2;
		height = 1;
		ClassifierMJ vertical = new ClassifierMJ(width, height);
		int halfWidth = (int) width / 2;
		Rectangle left = new Rectangle(0, 0, halfWidth, height);
		Rectangle right = new Rectangle(halfWidth, 0, halfWidth, height);
		vertical.addPlusPattern(left);
		vertical.addMinusPattern(right);
		vertical = (ClassifierMJ) vertical.getScaledInstance(15);
		vertical.setWeight(0.75);
		
		StrongClassifierMJ strong = new StrongClassifierMJ();
		strong.addWeakClassifier(horizontal);
		strong.addWeakClassifier(vertical);
		strong = (StrongClassifierMJ) strong.getScaledInstance(2);
     	
     	// wie groß ist der Klassifier
		Rectangle area = strong.getArea();

		// durchlaufe das Bild, ignoriere die Ränder
     	for (int y = 0; y < srcHeight-area.getHeight()*0.8; y++) {	
			for (int x = 0; x < srcWidth-area.getWidth()*0.8; x++)	{
				int pos = y * srcWidth + x;
				
				// berechne den Korrelationswert an jeder Position
				double correlation = strong.matchAt(image, x, y, threshold);
				
				// zeichne das Korrelationsbild
				int grey = (int)(correlation * 255.0);
				dstPixels[pos] =  (0xFF << 24) | (grey << 16) | (grey << 8) | grey;	
			}
		}
     	
     	// TODO finde die Maximas im Korrelations-Bild
     	Rectangle faceRect = image.getFaceRectangles().get(0); // ACHTUNG: vorgegebene Region
     	
     	// erstelle eine Kopie vom Eingangsbild
		BufferedImage bufferedImage = new BufferedImage(srcWidth, srcHeight, BufferedImage.TYPE_INT_ARGB);
    	bufferedImage.setRGB(0, 0, srcWidth, srcHeight, srcPixels, 0, srcWidth);
    	Graphics2D g2d = bufferedImage.createGraphics();
    	
    	// zeichne die Gesichtsregionen ein
    	// TODO verwende hier die gefundenen Maximas
     	strong.drawAt(g2d, (int)faceRect.getX(), (int)faceRect.getY());
     	
     	// schreibe die Kopie in die Eingangspixel zurück
    	g2d.dispose();
		bufferedImage.getRGB(0, 0, srcWidth, srcHeight, srcPixels, 0, srcWidth);  
    }    
}
