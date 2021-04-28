package de.tiramon.du.map;

import java.io.InputStream;
import java.nio.file.Path;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bell.oauth.discord.main.OAuthBuilder;
import bell.oauth.discord.main.Response;
import de.tiramon.du.map.model.Scanner;
import de.tiramon.du.map.model.Scanner.ScannerState;
import de.tiramon.du.map.model.Sound;
import de.tiramon.du.map.service.Service;
//import de.tiramon.du.event.LoginDoneEvent;
import de.tiramon.du.map.thread.FileReader;
import de.tiramon.du.map.thread.NewFileWatcher;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class DuMapDialog extends Application {
	protected Logger log = LoggerFactory.getLogger(getClass());

	private OAuthBuilder builder = InstanceProvider.getOAuthBuilder();
	private Service service = InstanceProvider.getService();

	Thread fileReaderThread;
	Thread fileWatcherThread;

	private NewFileWatcher newFileWatcher;
	private FileReader fileReader;

	public static boolean init = false;

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("DuMap Companion");
		InputStream in = getClass().getClassLoader().getResourceAsStream("favicon.png");
		primaryStage.getIcons().add(new Image(in));

		String token = builder.getAccess_token();
		if (token == null) {
			WebView wv = new WebView();
			wv.setContextMenuEnabled(false);
			WebEngine we = wv.getEngine();
			String authUrl = builder.getAuthorizationUrl(null);

			we.locationProperty().addListener((observable, oldValue, newValue) -> {
				String location = newValue;
				int index = location.indexOf("code=");
				if (index >= 0) {
					String code = location.substring(index + 5);
					Response response = builder.exchange(code);
					bell.oauth.discord.domain.User user = builder.getUser();
					log.info("Authenticated as {}#{}", user.getUsername(), user.getDiscriminator());
					primaryStage.setTitle("DualUniverse Companion - " + user.getUsername() + "#" + user.getDiscriminator());
					primaryStage.setScene(null);
					loginDone(primaryStage);
				}
			});
			we.load(authUrl);

			Scene scene = new Scene(wv, 500, 750);
			primaryStage.setScene(scene);
			primaryStage.show();
		} else {
			bell.oauth.discord.domain.User user = builder.getUser();
			log.info("Authenticated as {}#{}", user.getUsername(), user.getDiscriminator());
			primaryStage.setScene(null);
			loginDone(primaryStage);
		}
	}

	@Override
	public void stop() throws Exception {
		if (newFileWatcher != null) {
			newFileWatcher.stop();
		}
		if (fileReader != null) {
			fileReader.stop();
		}
		super.stop();
		Platform.exit();
		System.exit(0);
	}

	private void loginDone(Stage primaryStage) {
		Label logfileLabel = new Label();
		logfileLabel.setPrefWidth(600);

		newFileWatcher = new NewFileWatcher();
		ReadOnlyObjectProperty<Path> currentLogfileProperty = newFileWatcher.getCurrentLogFileProperty();
		currentLogfileProperty.addListener((ObservableValue<? extends Path> observable, Path oldValue, Path newValue) -> {
			// TODO Auto-generated method stub
			Platform.runLater(() -> logfileLabel.setText(newValue.toString()));
		});
		fileReader = new FileReader(currentLogfileProperty);
		fileReaderThread = new Thread(fileReader, "FileReader");
		fileReaderThread.start();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		fileWatcherThread = new Thread(newFileWatcher, "FileWatcher");

		fileWatcherThread.start();

		VBox pane = new VBox(10);

		HBox hbox = new HBox(10);
		ChoiceBox<Sound> soundChoice = new ChoiceBox<>();
		hbox.getChildren().add(soundChoice);

		soundChoice.setItems(service.getSoundOptions());
		soundChoice.valueProperty().bindBidirectional(service.currentSoundProperty());

		Button playButton = new Button(">");
		playButton.setOnAction(event -> service.playSound());
		hbox.getChildren().add(playButton);

		TableColumn<Scanner, Number> idColumn = new TableColumn<>("Scanner Id");
		idColumn.setCellValueFactory(param -> param.getValue().idProperty());

		TableColumn<Scanner, ScannerState> stateColumn = new TableColumn<>("State");
		stateColumn.setCellValueFactory(param -> param.getValue().stateProperty());
		stateColumn.setPrefWidth(60);

		TableColumn<Scanner, String> posColumn = new TableColumn<>("pos Link");
		posColumn.setCellValueFactory(param -> param.getValue().positionProperty());
		posColumn.setPrefWidth(200);

		TableColumn<Scanner, Number> timeLeftColumn = new TableColumn<>("time left");
		timeLeftColumn.setCellValueFactory(param -> param.getValue().timeLeftProperty());
		timeLeftColumn.setCellFactory(e -> new TableCell<Scanner, Number>() {
			@Override
			protected void updateItem(Number item, boolean empty) {
				super.updateItem(item, empty);

				if (item == null || empty) {
					setText(null);
				} else {
					long min = item.intValue() / 1000 / 60;
					long sec = item.intValue() / 1000 % 60;
					setText((min < 10 ? "0" : "") + min + ":" + (sec < 10 ? "0" : "") + sec);
				}
			}
		});
		timeLeftColumn.setPrefWidth(40);

		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy HH:mm:ss z");
		TableColumn<Scanner, Number> lastStateChangeColumn = new TableColumn<>("last change");
		lastStateChangeColumn.setCellValueFactory(param -> param.getValue().lastStateChangeProperty());
		lastStateChangeColumn.setCellFactory(e -> new TableCell<Scanner, Number>() {
			@Override
			protected void updateItem(Number item, boolean empty) {
				super.updateItem(item, empty);

				if (item == null || empty) {
					setText(null);
				} else {
					setText(sdf.format(item));
				}
			}

		});
		lastStateChangeColumn.setPrefWidth(140);

		TableView<Scanner> scannerList = new TableView<>();
		scannerList.getColumns().add(idColumn);
		scannerList.getColumns().add(posColumn);
		scannerList.getColumns().add(stateColumn);
		scannerList.getColumns().add(timeLeftColumn);
		scannerList.getColumns().add(lastStateChangeColumn);

		scannerList.setItems(service.getScannerList());

		pane.getChildren().add(logfileLabel);
		pane.getChildren().add(hbox);
		pane.getChildren().add(scannerList);

		Scene scene = new Scene(pane);
		primaryStage.setScene(scene);
		primaryStage.show();
	}
}
