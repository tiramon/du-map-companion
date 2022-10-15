package de.tiramon.du;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bell.oauth.discord.main.OAuthBuilder;
import bell.oauth.discord.main.Response;
import de.tiramon.du.map.model.Scanner;
import de.tiramon.du.map.model.Scanner.ScannerState;
import de.tiramon.du.map.model.Sound;
import de.tiramon.du.service.Service;
import de.tiramon.du.sound.service.SoundService;
import de.tiramon.du.tools.thread.ThreadInitializer;
import de.tiramon.du.utils.MethodEnumConverter;
import de.tiramon.github.update.service.UpdateService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
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

public class DuDialog extends Application {
	protected Logger log = LoggerFactory.getLogger(getClass());

	private OAuthBuilder builder = InstanceProvider.getOAuthBuilder();
	private Service service = InstanceProvider.getService();
	private SoundService soundService = InstanceProvider.getSoundService();
	private UpdateService updateService = InstanceProvider.getUpdateService();

	public static boolean init = false;

	private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy HH:mm:ss z");

	@Override
	public void start(Stage primaryStage) throws Exception {
		String version = updateService.getSemanticVersion().toString();
		primaryStage.setTitle("DualUniverse Companion - v" + version);
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
		ThreadInitializer.stopThreads();
		super.stop();
		Platform.exit();
		System.exit(0);
	}

	private void loginDone(Stage primaryStage) {
		ThreadInitializer.initThreads(service, new MethodEnumConverter(), InstanceProvider.getProperties());

		Label logfileLabel = new Label("No logfile found");
		logfileLabel.setPrefWidth(600);
		service.currentLogfileNameProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
			Platform.runLater(() -> logfileLabel.setText(newValue));
		});

		Label backlogLabel = new Label("Most recent Logfile");
		service.backlogCountProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
			Platform.runLater(() -> backlogLabel.setText(newValue.longValue() > 0 ? newValue + " newer logfiles" : "Most recent logfile"));
		});

		VBox pane = new VBox(10);

		HBox hbox = new HBox(10);
		if (InstanceProvider.isFeatureActive(Feature.SCANNER)) {
			ChoiceBox<Sound> soundChoice = new ChoiceBox<>();
			hbox.getChildren().add(soundChoice);

			soundChoice.setItems(service.getSoundOptions());
			soundChoice.valueProperty().bindBidirectional(service.currentSoundProperty());

			Button playButton = new Button(">");
			playButton.setOnAction(event -> service.playSound());
			hbox.getChildren().add(playButton);
		}

		hbox.getChildren().add(new Label("Last entry read:"));
		Label lastEntryReadLabel = new Label();
		lastEntryReadLabel.textProperty().bind(Bindings.createStringBinding(() -> {
			return sdf.format(new Date(service.lastEntryReadProperty().get()));
		}, service.lastEntryReadProperty()));
		hbox.getChildren().add(lastEntryReadLabel);

		hbox.getChildren().add(new Label("Idle:"));
		Circle working = new Circle();
		double stroke = 0;
		double radius = 7.;

		working.setStrokeWidth(stroke);
		working.setRadius(radius);
		working.setCenterX(radius + stroke);
		working.setCenterY(radius + stroke);

		working.setStroke(service.workingProperty().get() ? Color.ORANGE : Color.DARKGREEN);
		working.setFill(service.workingProperty().get() ? Color.YELLOW : Color.GREEN);
		Group workingGroup = new Group(working);
		service.workingProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
			working.setFill(newValue ? Color.YELLOW : Color.GREEN);
			working.setStroke(newValue ? Color.ORANGE : Color.DARKGREEN);
		});
		hbox.getChildren().add(workingGroup);

		Label analyzedLines = new Label("0 Lines");
		analyzedLines.textProperty().bind(Bindings.createStringBinding(() -> service.analyzedLinesProperty().get() + " lines", service.analyzedLinesProperty()));
		hbox.getChildren().add(analyzedLines);

		Label outstandingRequests = new Label("0 outstanding requests");
		outstandingRequests.textProperty().bind(Bindings.createStringBinding(() -> service.getMarketService().outstandingRequestsProperty().get() + " outstanding requests", service.getMarketService().outstandingRequestsProperty()));
		hbox.getChildren().add(outstandingRequests);

		CheckBox alwaysOnTopCheckbox = new CheckBox("Always on top");
		alwaysOnTopCheckbox.setSelected(primaryStage.isAlwaysOnTop());
		alwaysOnTopCheckbox.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
			primaryStage.setAlwaysOnTop(newValue);
		});

		VBox options = new VBox(10);
		options.getChildren().add(alwaysOnTopCheckbox);

		if (InstanceProvider.isFeatureActive(Feature.SOUND)) {
			CheckBox soundFrameworkCheckbox = new CheckBox("SoundFramework");
			soundFrameworkCheckbox.setSelected(soundService.isEnabled());
			soundFrameworkCheckbox.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
				soundService.setEnabled(newValue);
			});

			Slider volumeSlider = new Slider(0., 100., 100.);
			volumeSlider.setMajorTickUnit(25.0);
			volumeSlider.setMinorTickCount(100);
			volumeSlider.setShowTickMarks(true);
			volumeSlider.setShowTickLabels(true);
			volumeSlider.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
				soundService.setBaseVolume(newValue.doubleValue() / 100.);
			});
			volumeSlider.setValue(Double.valueOf(InstanceProvider.getProperties().getProperty("sound.framework.volume", "100")));

			options.getChildren().add(soundFrameworkCheckbox);
			options.getChildren().add(volumeSlider);
		}
		hbox.getChildren().add(options);

		boolean updateAvailable = updateService.isUpdateAvailable();
		if (updateAvailable) {
			addUpdate(pane.getChildren());
		} else {
			log.info("Version is already up to date");
		}

		pane.getChildren().add(logfileLabel);
		pane.getChildren().add(backlogLabel);
		pane.getChildren().add(hbox);
		if (InstanceProvider.isFeatureActive(Feature.SCANNER)) {
			TableView<Scanner> scannerList = createScannerList();
			VBox.setVgrow(scannerList, Priority.ALWAYS);
			pane.getChildren().add(scannerList);
		}

		Scene scene = new Scene(pane);
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	private void addUpdate(ObservableList<javafx.scene.Node> observableList) {
		HBox updateBox = new HBox(10);
		updateBox.setAlignment(Pos.BASELINE_CENTER);
		Hyperlink releaseLink = new Hyperlink();
		releaseLink.setText("Release");
		releaseLink.setOnAction((event) -> getHostServices().showDocument(updateService.getGithubReleaseInformation().getHtmlUrl()));
		Hyperlink jarLink = new Hyperlink();
		jarLink.setText("Jar");
		jarLink.setOnAction((event) -> getHostServices().showDocument(updateService.getGithubReleaseInformation().getAssets()[0].getBrowserDownloadUrl()));
		log.info("Update available");

		Label updateAvailableLabel = new Label("Update available " + InstanceProvider.getUpdateService().getSemanticGithubVersion());
		updateAvailableLabel.setStyle("-fx-font-weight: bold");
		updateBox.getChildren().add(updateAvailableLabel);
		updateBox.getChildren().add(releaseLink);
		updateBox.getChildren().add(jarLink);

		observableList.add(updateBox);
	}

	private TableView<Scanner> createScannerList() {
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
		lastStateChangeColumn.setSortType(SortType.DESCENDING);

		TableView<Scanner> scannerList = new TableView<>();

		scannerList.getColumns().add(idColumn);
		scannerList.getColumns().add(posColumn);
		scannerList.getColumns().add(stateColumn);
		scannerList.getColumns().add(timeLeftColumn);
		scannerList.getColumns().add(lastStateChangeColumn);

		Comparator<Scanner> scannerCompare = (Scanner scanner1, Scanner scanner2) -> {
			return (int) (scanner1.lastStateChangeProperty().get() - scanner2.lastStateChangeProperty().get());
		};
		SortedList sortedList = new SortedList<>(service.getScannerList(), scannerCompare);
		sortedList.comparatorProperty().bind(scannerList.comparatorProperty());

		scannerList.getSortOrder().add(lastStateChangeColumn);

		scannerList.setItems(sortedList);

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
		return scannerList;
	}
}
