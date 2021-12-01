package de.tiramon.du.map.service;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.collections.ObservableListWrapper;

import de.tiramon.du.map.DuMapDialog;
import de.tiramon.du.map.InstanceProvider;
import de.tiramon.du.map.model.ClaimType;
import de.tiramon.du.map.model.DUMethodsMap;
import de.tiramon.du.map.model.Ore;
import de.tiramon.du.map.model.Scan;
import de.tiramon.du.map.model.Scanner;
import de.tiramon.du.map.model.Scanner.ScannerState;
import de.tiramon.du.map.model.Sound;
import de.tiramon.du.map.model.User;
import de.tiramon.du.map.sound.SoundCommand;
import de.tiramon.du.map.sound.SoundConfig;
import de.tiramon.du.tools.model.DuLogRecord;
import de.tiramon.du.tools.service.IHandleService;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class Service implements IHandleService {
	protected Logger log = LoggerFactory.getLogger(getClass());

	private StringProperty currentLogfileName = new SimpleStringProperty();
	private BooleanProperty isInitializedProperty = new SimpleBooleanProperty();
	private BooleanProperty isWorkingProperty = new SimpleBooleanProperty();
	private LongProperty lastEntryReadProperty = new SimpleLongProperty();
	private LongProperty backlogCountProperty = new SimpleLongProperty();

	private SoundService soundService = InstanceProvider.getSoundService();
	private String soundSetting = InstanceProvider.getProperties().getProperty("territoryscan.sound");

	private DuMapService dumapService = new DuMapService();

	private Pattern asset = Pattern.compile("onTerritoryClaimed\\(planet=(?<planetId>\\d+), tile=(?<tileId>\\d+)\\)");
	private Pattern asset2 = Pattern.compile("Received rights for asset AssetId:\\[type = Territory, construct = ConstructId:\\[constructId = 0\\], element = ElementId:\\[elementId = 0\\], territory = TerritoryTileIndex:\\[planetId = (?<planetId>\\d+), tileIndex = (?<tileId>\\d+)\\], item = ItemId:\\[typeId = 0, instanceId = 0, ownerId = EntityId:\\[playerId = 0, organizationId = 0\\]\\], organization = OrganizationId:\\[organizationId = 0\\]\\]");
	private Pattern assetReleased = Pattern.compile("onTerritoryReleased\\(planet=(?<planetId>\\d+), tile=(?<tileId>\\d+)\\)");
	private Pattern userPattern = Pattern.compile("LoginResponse:\\[playerId = (?<playerId>\\d+), username = (?<playerName>.+?), communityId = (?<communityId>\\d+), ip = [\\d+\\.\\|]+, timeSeconds = @\\((?<loginTimestamp>\\d+)\\) \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\]");

	private Pattern scanner_start = Pattern.compile("TerritoryScan\\[(?<scannerid>\\d+)\\] starts");
	private Pattern scanner_stop = Pattern.compile("AssetEvent: Played sound event Construct_Element_TerritoryScanner_Stop -- id (?<scannerid>\\d+)");
	private Pattern scanner_result_position = Pattern.compile("TerritoryScan\\[\\d+#(?<scannerid>\\d+)\\] end: lasted (\\d+\\.\\d+) seconds coordinates: (?<position>::pos\\{\\d+,\\d+,-?\\d+\\.\\d+,-?\\d+\\.\\d+,-?\\d+\\.\\d+})");
	private Pattern scanner_reset = Pattern.compile("Coroutine \\d+\\(TerritoryScan_(?<constructid>\\d+)#(?<scannerid>\\d+)\\) killed");
	private Pattern scanOrePattern = Pattern.compile("TerritoryScan\\[(?<scannerid>\\d+)\\]: material: (?<oreName>[A-Za-z ]+) : (?<oreAmount>[\\de+\\.]+) \\* \\d+");

	/*
	 * sound_play|path_to/the.mp3(string)|ID(string)|Optional Volume(int 0-100) -- Plays a concurrent sound sound_notification|path_to/the.mp3(string)|ID(string)|Optional Volume(int 0-100) -- Lowers volume on all other sounds for its duration, and plays overtop sound_q|path_to/the.mp3(string)|ID(string)|Optional Volume(int 0-100) -- Plays a sound after all other queued sounds finish
	 *
	 * -- The following use the IDs that were specified in the previous three
	 *
	 * sound_volume|ID(string)|Volume(int 0-100)
	 *
	 * sound_pause|Optional ID(string) -- If no ID is specified, pauses all sounds sound_stop|Optional ID(string) -- If no ID is specified, stops all sounds sound_resume|Optional ID(string) -- If no ID is specified, resumes all paused sounds
	 */

	private Pattern soundCommand = Pattern.compile("^(?<command>sound_notification|sound_play|sound_q|sound_pause|sound_stop|sound_resume|sound_volume|sound_loop)\\|");

	private User user;
	private Set<Long> knownAssets = ConcurrentHashMap.newKeySet();

	private ObjectProperty<Sound> currentSoundProperty = new SimpleObjectProperty<>();
	private List<Sound> soundList = new ArrayList<>();

	private Map<Long, Scan> currentScans = new HashMap<>();
	private ConcurrentHashMap<Long, Scanner> scannerMap = new ConcurrentHashMap<>();
	private ObservableList<Scanner> list = FXCollections.observableList(new CopyOnWriteArrayList<>(), item -> new Observable[] { item.timeLeftProperty(), item.positionProperty(), item.stateProperty() });

	public Service() {
		initSound();
	}

	public void initSound() {
		soundList.add(new Sound("None", null));
		soundList.add(new Sound("Pling", "35631__reinsamba__crystal-glass.mp3"));
		soundList.add(new Sound("Alien Voice", "territoryscancompleted_alienvoice.mp3"));
		log.info("Territory Scanner analysis activated");
		currentSoundProperty.set(soundList.get(1));

		if (soundSetting != null && !soundSetting.isBlank()) {
			Sound sound = soundList.stream().filter(s -> s.getTitle().equalsIgnoreCase(soundSetting)).findFirst().orElse(null);
			if (sound != null) {
				currentSoundProperty.set(sound);
			}
		}
	}

	public void handleAsset(DuLogRecord record) {
		Matcher matcher = asset.matcher(record.message);
		if (matcher.matches()) {
			long planetId = Long.valueOf(matcher.group("planetId"));
			long tileId = Long.valueOf(matcher.group("tileId"));
			log.info("received asset message for {} on {}", tileId, planetId);
			knownAssets.add(planetId * 1000000 + tileId);
			dumapService.sendClaimedTile(planetId, tileId, record.millis, ClaimType.CLAIM);
		} else {
			int bp = 0;
		}
	}

	public void handleScanStatusChange(DuLogRecord record) {
		if (record.method.equals(DUMethodsMap.TERRITORYSCANNER_RESET)) {
			Matcher matcher = scanner_reset.matcher(record.message);
			if (matcher.matches()) {
				long scannerid = Long.valueOf(matcher.group("scannerid"));
				scannerMap.get(scannerid).setState(ScannerState.RESETED, record.millis);
				log.info("Scanner " + scannerid + " reseted");
				return;
			}
			return;
		}
		{
			Matcher matcher = scanner_start.matcher(record.message);
			if (matcher.matches()) {
				long scannerid = Long.valueOf(matcher.group("scannerid"));
				Scanner scanner = new Scanner(scannerid);
				if (scannerMap.putIfAbsent(scannerid, scanner) == null) {
					log.info("new scanner {}", scannerid);
					list.add(scanner);
				}
				scannerMap.get(scannerid).setState(ScannerState.STARTED, record.millis);
				scannerMap.get(scannerid).setSubmited(false);
				log.info("Scanner " + scannerid + " started");
				return;
			} else {
				int bp = 0;
			}
		}

		/*
		 * {// TODO remove Matcher matcher = scanner_stop.matcher(record.message); if (matcher.matches()) { long scannerid = Long.valueOf(matcher.group("scannerid")); scannerMap.get(scannerid).setState(ScannerState.FINISHED, record.millis); log.info("Scanner " + scannerid + " finished"); scannerMap.get(scannerid).setSubmited(false); playSound(); return; } else { int bp = 0; } }
		 */
	}

	public void handleScanPosition(DuLogRecord record) {
		Matcher matcher = scanner_result_position.matcher(record.message);
		if (matcher.matches()) {
			long scannerid = Long.valueOf(matcher.group("scannerid"));

			String position = matcher.group("position").replace("-0.0000", "0.0000");
			Scanner scanner = scannerMap.get(scannerid);

			scanner.setState(ScannerState.FINISHED, record.millis);
			scanner.setSubmited(false);
			scanner.setPosition(position);
			playSound();
			Scan scan = new Scan(position, record.millis);

			log.info("Scanner " + scannerid + " at postion " + position);
			currentScans.put(scannerid, scan);
		} else {
			int bp = 0;
		}
	}

	public void handleScanOre(DuLogRecord record) {
		Matcher matcher = scanOrePattern.matcher(record.message);
		if (matcher.find()) {
			final long scannerId = Long.parseLong(matcher.group("scannerid"));

			Ore ore = Ore.byName(matcher.group("oreName"));
			long oreAmount = Double.valueOf(matcher.group("oreAmount")).longValue();
			Scan scan = currentScans.get(scannerId);
			if (scan != null) {
				if (scan.getOres().containsKey(ore)) {
					log.info("received redundant ore scan result, so scan is done");
					currentScans.remove(scannerId);
					dumapService.sendScan(scan.getPlanet(), scan.getLat(), scan.getLon(), scan.getTimestamp(), scan.getOres());
					scannerMap.get(scannerId).setSubmited(true);
				} else {
					scan.add(ore, oreAmount);
					log.info("Scanner {} found {} {}", scannerId, oreAmount, ore.getName());
				}
			} else {
				log.trace("received redundant ore scan result");
			}
		} else {
			int bp = 0;
		}
	}

	public void handleUser(DuLogRecord record) {
		Matcher matcher = userPattern.matcher(record.message);
		if (matcher.find()) {
			Long playerId = Long.parseLong(matcher.group("playerId"));
			Long communityId = Long.parseLong(matcher.group("communityId"));
			String playerName = matcher.group("playerName");
			user = new User(playerId, communityId, playerName);
			log.info("Player is id: {} communityId: {} name: {}", playerId, communityId, playerName);
		} else {
			throw new RuntimeException("Invalid Pattern");
		}
	}

	public void playSound() {
		if (!DuMapDialog.init) {
			return;
		}
		String strFilename = currentSoundProperty.get().getResourceFileName();
		if (strFilename != null) {
			URL url = getClass().getClassLoader().getResource(strFilename);
			Media hit = new Media(url.toString());

			MediaPlayer mediaPlayer = new MediaPlayer(hit);
			mediaPlayer.setVolume(soundService.baseVolumeProperty());
			mediaPlayer.play();
		}
		log.debug("played sound");
	}

	public ObservableList<Sound> getSoundOptions() {
		return new ObservableListWrapper<>(soundList);
	}

	public ObjectProperty<Sound> currentSoundProperty() {
		return currentSoundProperty;
	}

	public ObservableList<Scanner> getScannerList() {
		return list;
	}

	public void handleAssetRights(DuLogRecord record) {
		Matcher matcher = asset2.matcher(record.message);
		if (matcher.matches()) {
			long planetId = Long.valueOf(matcher.group("planetId"));
			long tileId = Long.valueOf(matcher.group("tileId"));
			log.info("received asset rights message for {} on {}", tileId, planetId);
			if (knownAssets.add(planetId * 1000000 + tileId)) {
				dumapService.sendClaimedTile(planetId, tileId, record.millis, ClaimType.UPDATE);
			}
		} else {
			int bp = 0;
		}
	}

	public void handleLogInfo(DuLogRecord record) {
		Matcher matcher = soundCommand.matcher(record.message);
		if (matcher.find()) {
			if (!DuMapDialog.init) {
				return;
			}
			SoundCommand command = SoundCommand.valueOf(matcher.group("command").split("_")[1].toUpperCase());
			String[] parameter = record.message.split("\\|");
			if (command == SoundCommand.PLAY || command == SoundCommand.LOOP) {
				File file = new File(parameter[1]);
				if (file.exists() && file.canRead()) {
					String id = parameter[2];
					Long volume = parameter.length < 4 ? 100 : Long.valueOf(parameter[3]);
					Platform.runLater(() -> soundService.playConcurrent(new SoundConfig(parameter[1], id, volume), command == SoundCommand.LOOP));
				} else {
					log.warn("Skipping current sound command '{}' because reference file does not exist or is not readable", record.message);
				}
			} else if (command == SoundCommand.NOTIFICATION) {
				File file = new File(parameter[1]);
				if (file.exists() && file.canRead()) {
					String id = parameter[2];
					Long volume = parameter.length < 4 ? 100 : Long.valueOf(parameter[3]);
					Platform.runLater(() -> soundService.addNotification(new SoundConfig(parameter[1], id, volume)));
				} else {
					log.warn("Skipping current sound command '{}' because reference file does not exist or is not readable", record.message);
				}
			} else if (command == SoundCommand.Q) {
				File file = new File(parameter[1]);
				if (file.exists() && file.canRead()) {
					String id = parameter[2];
					Long volume = parameter.length < 4 ? 100 : Long.valueOf(parameter[3]);
					Platform.runLater(() -> soundService.addToQueue(new SoundConfig(parameter[1], id, volume)));
				} else {
					log.warn("Skipping current sound command '{}' because reference file does not exist or is not readable", record.message);
				}
			} else if (command == SoundCommand.VOLUME) {
				String id = parameter[1];
				Long volume = Long.valueOf(parameter[2]);

			} else if (command == SoundCommand.PAUSE) {
				String id = parameter.length < 2 ? null : parameter[1];
				Platform.runLater(() -> soundService.pauseQueue(id));
			} else if (command == SoundCommand.STOP) {
				String id = parameter.length < 2 ? null : parameter[1];
				Platform.runLater(() -> soundService.stopQueue(id));
			} else if (command == SoundCommand.RESUME) {
				String id = parameter.length < 2 ? null : parameter[1];
				Platform.runLater(() -> soundService.resumeQueue(id));
			}
		}

	}

	public void handleAssetReleased(DuLogRecord record) {
		Matcher matcher = assetReleased.matcher(record.message);
		if (matcher.matches()) {
			long planetId = Long.valueOf(matcher.group("planetId"));
			long tileId = Long.valueOf(matcher.group("tileId"));
			log.info("received asset released message for {} on {}", tileId, planetId);
			knownAssets.remove(planetId * 1000000 + tileId);
			dumapService.sendClaimedTile(planetId, tileId, record.millis, ClaimType.RELEASE);

		} else {
			int bp = 0;
		}
	}

	@Override
	public void setCurrentLogfileName(Path newLogFile) {
		Platform.runLater(() -> currentLogfileName.set(newLogFile.toString()));
	}

	@Override
	public void setInitialized(final boolean b) {
		DuMapDialog.init = true;
		Platform.runLater(() -> isInitializedProperty.set(b));
	}

	@Override
	public void setWorking(final boolean b) {
		Platform.runLater(() -> isWorkingProperty.set(b));
	}

	@Override
	public void setLastEntryRead(final long timestamp) {
		Platform.runLater(() -> lastEntryReadProperty.set(timestamp));
	}

	@Override
	public void setBacklogCount(final long count) {
		Platform.runLater(() -> backlogCountProperty.set(count));
	}

	public BooleanProperty workingProperty() {
		return isWorkingProperty;
	}

	public BooleanProperty initializedProperty() {
		return isInitializedProperty;
	}

	public StringProperty currentLogfileNameProperty() {
		return currentLogfileName;
	}

	public LongProperty lastEntryReadProperty() {
		return lastEntryReadProperty;
	}

	public LongProperty backlogCountProperty() {
		return backlogCountProperty;
	}

	@Override
	public void handle(de.tiramon.du.tools.model.DuLogRecord record) {
		if (record.method.equals(DUMethodsMap.ASSET_CLAIM)) {
			handleAsset(record);
		} else if (record.method.equals(DUMethodsMap.TERRITORYSCANNER_STATUSCHANGE)) {
			handleScanStatusChange(record);
		} else if (record.method.equals(DUMethodsMap.TERRITORYSCANNER_STATUSSTART)) {
			handleScanStatusChange(record);
		} else if (record.method.equals(DUMethodsMap.TERRITORYSCANNER_RESULT)) {
			handleScanOre(record);
		} else if (record.method.equals(DUMethodsMap.TERRITORYSCANNER_POSITION)) {
			handleScanPosition(record);
		} else if (record.method.equals(DUMethodsMap.TERRITORYSCANNER_RESET)) {
			handleScanStatusChange(record);
		} else if (record.method.equals(DUMethodsMap.ASSET_RIGHTS)) {
			handleAssetRights(record);
		} else if (record.method.equals(DUMethodsMap.ASSET_RELEASED)) {
			handleAssetReleased(record);
		} else if (record.method.equals(DUMethodsMap.LOG_INFO)) {
			handleLogInfo(record);
		}
	}
}
