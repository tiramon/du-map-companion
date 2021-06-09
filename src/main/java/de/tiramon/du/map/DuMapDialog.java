package de.tiramon.du.map;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bell.oauth.discord.main.OAuthBuilder;
import bell.oauth.discord.main.Response;
import de.tiramon.du.map.model.Scanner;
import de.tiramon.du.map.model.Scanner.ScannerState;
import de.tiramon.du.map.model.Sound;
import de.tiramon.du.map.service.Service;
import de.tiramon.du.map.thread.FileReader;
import de.tiramon.du.map.thread.NewFileWatcher;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class DuMapDialog extends Application {
	protected Logger log = LoggerFactory.getLogger(getClass());

	private OAuthBuilder builder = InstanceProvider.getOAuthBuilder();
	private Service service = InstanceProvider.getService();

	private Thread fileReaderThread;
	private Thread fileWatcherThread;

	private NewFileWatcher newFileWatcher;
	private FileReader fileReader;

	public static boolean init = false;

	@Override
	public void start(Stage primaryStage) throws Exception {
		Manifest manifest = getManifest(this.getClass());
		String version;
		if (manifest != null) {
			version = getVersion(manifest);
		} else {
			version = "?.?.?";
		}
		primaryStage.setTitle("DuMap Companion - v" + version);
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
					primaryStage.setTitle("DualUniverse Companion - v" + version + " - " + user.getUsername() + "#" + user.getDiscriminator());
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
			primaryStage.setTitle("DualUniverse Companion - v" + version + " - " + user.getUsername() + "#" + user.getDiscriminator());
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
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy HH:mm:ss z");
		Label logfileLabel = new Label();
		logfileLabel.setPrefWidth(600);

		fileReader = new FileReader();

		newFileWatcher = new NewFileWatcher(fileReader.getQueue());
		ReadOnlyObjectProperty<Path> currentLogfileProperty = newFileWatcher.getCurrentLogFileProperty();
		currentLogfileProperty.addListener((ObservableValue<? extends Path> observable, Path oldValue, Path newValue) -> {
			// TODO Auto-generated method stub
			Platform.runLater(() -> logfileLabel.setText(newValue.toString()));
		});

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

		hbox.getChildren().add(new Label("Last entry read:"));
		Label lastEntryReadLabel = new Label();
		lastEntryReadLabel.textProperty().bind(Bindings.createStringBinding(() -> {
			return sdf.format(new Date(fileReader.getLastEntryReadProperty().get()));
		}, fileReader.getLastEntryReadProperty()));
		hbox.getChildren().add(lastEntryReadLabel);

		hbox.getChildren().add(new Label("Idle:"));
		Circle working = new Circle();
		double stroke = 0;
		double radius = 7.;

		working.setStrokeWidth(stroke);
		working.setRadius(radius);
		working.setCenterX(radius + stroke);
		working.setCenterY(radius + stroke);

		working.setStroke(fileReader.isWorkingProperty().get() ? Color.DARKGREEN : Color.ORANGE);
		working.setFill(fileReader.isWorkingProperty().get() ? Color.GREEN : Color.YELLOW);
		Group workingGroup = new Group(working);
		fileReader.isWorkingProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
			working.setFill(newValue ? Color.YELLOW : Color.GREEN);
			working.setStroke(newValue ? Color.ORANGE : Color.DARKGREEN);
		});
		hbox.getChildren().add(workingGroup);

		CheckBox alwaysOnTopCheckbox = new CheckBox("Always on top");
		alwaysOnTopCheckbox.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
			primaryStage.setAlwaysOnTop(newValue);
		});

		hbox.getChildren().add(alwaysOnTopCheckbox);
		TableColumn<Scanner, Number> idColumn = new TableColumn<>("Scanner Id");
		idColumn.setCellValueFactory(param -> param.getValue().idProperty());

		TableColumn<Scanner, ScannerState> stateColumn = new TableColumn<>("State");
		stateColumn.setCellValueFactory(param -> param.getValue().stateProperty());
		stateColumn.setStyle("-fx-alignment: CENTER;");
		stateColumn.setCellFactory(e -> new TableCell<Scanner, ScannerState>() {
			@Override
			protected void updateItem(ScannerState item, boolean empty) {
				super.updateItem(item, empty);

				if (item == null || empty) {
					setText(null);
				} else {
					setText(item.toString());

					if (item == ScannerState.SUBMITED) {
						this.setStyle("-fx-alignment: CENTER;-fx-background-color: lightgreen;");
					} else if (item == ScannerState.RESETED) {
						this.setStyle("-fx-alignment: CENTER;-fx-background-color: #ffcccb;");
					} else if (item == ScannerState.STARTED) {
						this.setStyle("-fx-alignment: CENTER;-fx-background-color: yellow;");
					} else {
						this.setStyle("-fx-alignment: CENTER;");
					}
				}
			}
		});
		stateColumn.setPrefWidth(65);

		TableColumn<Scanner, String> posColumn = new TableColumn<>("pos Link");
		posColumn.setCellValueFactory(param -> param.getValue().positionProperty());
		posColumn.setPrefWidth(200);

		TableColumn<Scanner, Number> timeLeftColumn = new TableColumn<>("time left");
		timeLeftColumn.setCellValueFactory(param -> param.getValue().timeLeftProperty());
		timeLeftColumn.setStyle("-fx-alignment: CENTER;");
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

		scannerList.setOnMouseClicked(event -> {
			Scanner entry = scannerList.getSelectionModel().getSelectedItem();
			if (entry == null) {
				return;
			}
			final Clipboard clipboard = Clipboard.getSystemClipboard();
			final ClipboardContent content = new ClipboardContent();
			content.putString(entry.positionProperty().get());
			clipboard.setContent(content);
		});

		pane.getChildren().add(logfileLabel);
		pane.getChildren().add(hbox);
		VBox.setVgrow(scannerList, Priority.ALWAYS);
		pane.getChildren().add(scannerList);

		Scene scene = new Scene(pane);
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static Manifest getManifest(Class<?> theClass) throws IOException {

		// Find the path of the compiled class
		String classPath = theClass.getResource(theClass.getSimpleName() + ".class").toString();

		// Find the path of the lib which includes the class
		if (classPath.lastIndexOf('!') == -1) {
			return null;
		}
		String libPath = classPath.substring(0, classPath.lastIndexOf('!'));
		if (libPath.endsWith("/BOOT-INF/classes")) {
			libPath = libPath.substring(0, libPath.lastIndexOf('!'));
		}
		// Find the path of the file inside the lib jar
		String filePath = libPath + "!/META-INF/MANIFEST.MF";

		// We look at the manifest file, getting three attributes out of it
		return new Manifest(new URL(filePath).openStream());
	}

	public static String getVersion(Manifest manifest) {
		if (manifest == null) {
			return "?.?.?";
		}
		Attributes attr = manifest.getMainAttributes();
		return attr.getValue("Implementation-Version");
	}

}
