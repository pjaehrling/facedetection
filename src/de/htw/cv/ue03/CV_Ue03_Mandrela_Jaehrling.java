/**
 * @author Nico Hezel
 */
package de.htw.cv.ue03;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CV_Ue03_Mandrela_Jaehrling extends Application {

	@Override
	public void start(Stage stage) throws Exception {
				
		Parent ui = new FXMLLoader(getClass().getResource("FaceDetectionView.fxml")).load();
		Scene scene = new Scene(ui);
		stage.setScene(scene);
		stage.setTitle("Face Detection - M. Mandrela, P. Jährling");
		stage.show();
	}

	public static void main(String[] args) {
		launch(args);
	}
}