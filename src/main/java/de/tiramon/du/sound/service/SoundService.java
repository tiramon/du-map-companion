package de.tiramon.du.sound.service;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.SplittableRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tiramon.du.sound.SoundConfig;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;

public class SoundService {
	SplittableRandom random = new SplittableRandom();
	protected Logger log = LoggerFactory.getLogger(getClass());
	private Queue<SoundConfig> soundQueue = new LinkedBlockingDeque<>();
	private Queue<SoundConfig> notificationQueue = new LinkedBlockingDeque<>();

	private double concurrentVolume = 100;
	private double notificationVolume = 100;
	private double queueVolume = 100;

	private double volumeFactor = 1.0;
	private MediaPlayer queuePlayer = null;
	private MediaPlayer notificationPlayer = null;
	private boolean enabled;

	private List<MediaPlayer> concurrentPlayers = new CopyOnWriteArrayList<>();
	private Map<String, MediaPlayer> id2PlayerMap = new ConcurrentHashMap<>();

	private double baseVolume = 1.;

	public SoundService(boolean enabled) {
		this.enabled = enabled;
	}

	public void addToQueue(SoundConfig config) {
		log.info("adding new queue {} {}", config.getFile(), soundQueue.stream().map(SoundConfig::getId).collect(Collectors.joining(",")));
		removeId(config.getId());
		log.debug("queue size {}", soundQueue.size());
		soundQueue.add(config);
		log.debug("queue size {}", soundQueue.size());
		startQueue();
	}

	public void pauseQueue(String id) {
		if (id != null) {
			log.info("pausing id '{}'", id);
			MediaPlayer player = id2PlayerMap.get(id);
			if (player != null && player.getStatus() == Status.PLAYING) {
				player.pause();
			}
		} else {
			log.info("pausing all");
			if (queuePlayer != null && queuePlayer.getStatus() == Status.PLAYING) {
				queuePlayer.pause();
			}
			if (notificationPlayer != null && notificationPlayer.getStatus() == Status.PLAYING) {
				notificationPlayer.pause();
			}
			if (!concurrentPlayers.isEmpty()) {
				for (MediaPlayer player : concurrentPlayers) {
					if (player != null && player.getStatus() == Status.PLAYING) {
						player.pause();
					}
				}
			}
		}
	}

	public void resumeQueue(String id) {
		if (id != null) {
			log.info("resuming id '{}'", id);
			MediaPlayer player = id2PlayerMap.get(id);
			if (player != null && player.getStatus() == Status.PAUSED) {
				player.play();
			}
		} else {
			log.info("resuming all");
			if (queuePlayer != null && queuePlayer.getStatus() == Status.PAUSED) {
				queuePlayer.play();
			}
			if (notificationPlayer != null && notificationPlayer.getStatus() == Status.PAUSED) {
				notificationPlayer.play();
			}
			if (!concurrentPlayers.isEmpty()) {
				for (MediaPlayer player : concurrentPlayers) {
					if (player != null && player.getStatus() == Status.PAUSED) {
						player.play();
					}
				}
			}
		}
	}

	public void stopQueue(String id) {
		if (id != null) {
			log.info("stoping id '{}'", id);
			removeId(id);
		} else {
			log.info("stoping all");
			soundQueue.clear();
			if (queuePlayer != null && (queuePlayer.getStatus() == Status.PLAYING || queuePlayer.getStatus() == Status.PAUSED)) {
				queuePlayer.stop();
			}
			notificationQueue.clear();
			if (notificationPlayer != null && (notificationPlayer.getStatus() == Status.PLAYING || notificationPlayer.getStatus() == Status.PAUSED)) {
				notificationPlayer.stop();
			}
			if (!concurrentPlayers.isEmpty()) {
				for (MediaPlayer player : concurrentPlayers) {
					if (player != null && (player.getStatus() == Status.PLAYING || player.getStatus() == Status.PAUSED)) {
						player.stop();
					}
				}
			}
		}
	}

	public void startQueue() {
		log.debug("starting queue {}", soundQueue.size());
		if (!enabled) {
			log.debug("disabled");
			return;
		}
		if (queuePlayer != null && (queuePlayer.getStatus() == Status.PLAYING || queuePlayer.getStatus() == Status.PAUSED)) {
			log.debug("queue is already {}", queuePlayer.getStatus());
			return;
		}
		if (queuePlayer != null && (queuePlayer.getStatus() == Status.UNKNOWN || queuePlayer.getStatus() == Status.READY)) {
			log.debug("Status {}", queuePlayer.getStatus());
			return;
		}
		if (soundQueue.isEmpty()) {
			log.debug("queue empty");
			return;
		}
		if (queuePlayer != null && queuePlayer.getStatus() != Status.DISPOSED) {
			log.debug("stoping old queue player status '{}'", queuePlayer.getStatus());
			queuePlayer.stop();
			return;
		}

		log.debug("queue contains {} elements", soundQueue.size());
		SoundConfig config = soundQueue.poll();
		double calcedVolume = baseVolume * (queueVolume / 100) * volumeFactor;
		log.info("playing next sound in queue {} with volume {}", config.getFile().toString(), calcedVolume);

		queuePlayer = new MediaPlayer(new Media(getRandomFile(config.getFile())));
		queuePlayer.setVolume(calcedVolume);
		queuePlayer.setOnEndOfMedia(() -> {
			log.debug("queueplayer end");
			queuePlayer.stop();
		});
		queuePlayer.setOnStopped(() -> {
			log.debug("queueplayer stoped");
			queuePlayer.dispose();
			queuePlayer = null;
			id2PlayerMap.remove(config.getId());
			startQueue();
		});
		id2PlayerMap.put(config.getId(), queuePlayer);
		queuePlayer.play();
	}

	String getRandomFile(File file) {
		if (!file.canRead()) {
			log.warn("File '{}' can't be read", file.getAbsolutePath());
			return null;
		}
		if (file.isFile()) {
			return file.toURI().toString();
		} else {
			File[] folderContent = file.listFiles();

			int amountFiles = folderContent.length;
			if (amountFiles == 0) {
				log.warn("No file in referenced folder '{}'", file.getAbsolutePath());
				return null;
			}
			File randFile = folderContent[random.nextInt(amountFiles)];
			if (!randFile.canRead()) {
				log.warn("Rand file in referenced folder is not readable '{}'", randFile.getAbsolutePath());
				return null;
			} else {
				return randFile.toURI().toString();
			}
		}

	}

	public void notificationStart() {
		volumeFactor = 0.3;
		if (queuePlayer != null && queuePlayer.getStatus() == Status.PLAYING) {
			queuePlayer.setVolume(baseVolume * (queueVolume / 100) * volumeFactor);
		}
		if (!concurrentPlayers.isEmpty()) {
			for (MediaPlayer player : concurrentPlayers) {
				if (player != null && player.getStatus() == Status.PLAYING) {
					player.setVolume(baseVolume * (concurrentVolume / 100) * volumeFactor);
				}
			}
		}
	}

	public void notificationStop() {
		volumeFactor = 1.0;
		if (queuePlayer != null && queuePlayer.getStatus() == Status.PLAYING) {
			queuePlayer.setVolume(baseVolume * (queueVolume / 100) * volumeFactor);
		}
		if (!concurrentPlayers.isEmpty()) {
			for (MediaPlayer player : concurrentPlayers) {
				if (player != null && player.getStatus() == Status.PLAYING) {
					player.setVolume(baseVolume * (concurrentVolume / 100) * volumeFactor);
				}
			}
		}
	}

	public void addNotification(SoundConfig config) {
		log.debug("adding new notification");
		removeId(config.getId());
		notificationQueue.add(config);
		startNotification();
	}

	public void startNotification() {
		if (!enabled) {
			log.debug("disabled");
			return;
		}
		if (notificationPlayer != null && (notificationPlayer.getStatus() == Status.PLAYING || notificationPlayer.getStatus() == Status.PAUSED)) {
			return;
		}
		if (notificationQueue.isEmpty()) {
			log.debug("notification queue empty");
			return;
		}
		if (notificationPlayer != null && (notificationPlayer.getStatus() == Status.UNKNOWN || notificationPlayer.getStatus() == Status.READY)) {
			log.debug("Status {}", notificationPlayer.getStatus());
			return;
		}

		if (notificationPlayer != null && notificationPlayer.getStatus() != Status.DISPOSED) {
			notificationPlayer.stop();
			return;
		}

		SoundConfig config = notificationQueue.poll();
		double calcedVolume = baseVolume * notificationVolume / 100;
		log.info("playing next sound in notification queue {} with volume {}", config.getFile().toString(), calcedVolume);
		notificationPlayer = new MediaPlayer(new Media(getRandomFile(config.getFile())));
		notificationPlayer.setVolume(calcedVolume);
		notificationPlayer.setOnEndOfMedia(() -> {
			log.info("notification end");
			notificationPlayer.stop();

		});
		notificationPlayer.setOnStopped(() -> {
			log.debug("notification stop");
			if (notificationPlayer != null) {
				notificationPlayer.dispose();
				notificationStop();
				notificationPlayer = null;
				id2PlayerMap.remove(config.getId());
			}
			startNotification();
		});
		notificationStart();
		id2PlayerMap.put(config.getId(), notificationPlayer);
		notificationPlayer.play();
	}

	public void startQueue(final MediaPlayer mediaPlayer, Queue<SoundConfig> queue, boolean isNotification) {
		if (!enabled) {
			log.debug("disabled");
			return;
		}
		if (mediaPlayer != null && mediaPlayer.getStatus() == Status.PLAYING) {
			return;
		}
		if (queue.isEmpty()) {
			log.debug("queue empty");
			return;
		}
		if (!soundQueue.isEmpty() && (mediaPlayer == null || mediaPlayer.getStatus() == Status.STOPPED || mediaPlayer.getStatus() == Status.PAUSED || mediaPlayer.getStatus() == Status.HALTED || mediaPlayer.getStatus() == Status.DISPOSED)) {
			if (mediaPlayer != null && mediaPlayer.getStatus() != Status.DISPOSED) {
				mediaPlayer.stop();
			}

			SoundConfig config = soundQueue.poll();
			double calcedVolume = baseVolume * concurrentVolume / 100 * volumeFactor;
			// FIXME
			log.info("playing next sound in queue {} with volume {}", config.getFile().toString(), calcedVolume);
			MediaPlayer newMediaPlayer = new MediaPlayer(new Media(getRandomFile(config.getFile())));

			if (isNotification) {
				newMediaPlayer.setVolume(baseVolume * notificationVolume / 100);
				notificationStart();
			} else {
				newMediaPlayer.setVolume(baseVolume * queueVolume / 100 * volumeFactor);
			}
			newMediaPlayer.setOnEndOfMedia(() -> {
				log.debug("queue end");
				newMediaPlayer.stop();
				startQueue(mediaPlayer, queue, isNotification);
			});
			newMediaPlayer.setOnStopped(() -> {
				log.debug("queue stop");
				newMediaPlayer.dispose();
				if (isNotification) {
					notificationStop();
				}
				id2PlayerMap.remove(config.getId());
			});
			if (isNotification) {
				notificationPlayer = newMediaPlayer;
			} else {
				queuePlayer = newMediaPlayer;
			}
			mediaPlayer.play();
		}
	}

	public void playConcurrent(SoundConfig config, boolean loop) {
		if (!enabled) {
			log.debug("disabled");
			return;
		}
		double calcedVolume = baseVolume * concurrentVolume / 100 * volumeFactor;

		if (id2PlayerMap.containsKey(config.getId())) {
			if (loop) {
				return;
			} else {
				removeId(config.getId());
			}
		}
		log.info("play new concurrent {} with volume {}", config.getFile().toString(), calcedVolume);

		MediaPlayer concurrentPlayer = new MediaPlayer(new Media(getRandomFile(config.getFile())));
		concurrentPlayer.setVolume(calcedVolume);
		concurrentPlayer.setOnEndOfMedia(() -> {
			concurrentPlayer.stop();
		});
		concurrentPlayer.setOnStopped(() -> {
			concurrentPlayer.dispose();
			concurrentPlayers.remove(concurrentPlayer);
			id2PlayerMap.remove(config.getId());
		});
		concurrentPlayers.add(concurrentPlayer);
		id2PlayerMap.put(config.getId(), concurrentPlayer);
		concurrentPlayer.play();
	}

	public void setEnabled(Boolean newValue) {
		enabled = newValue != null ? newValue : false;
		log.info("Soundframework {}", enabled ? "enabled" : "disabled");
	}

	public boolean isEnabled() {
		return enabled;
	}

	private void removeId(String id) {
		log.debug("removing id '{}'", id);
		Optional<SoundConfig> previousIdSound = soundQueue.stream().filter(sound -> sound.getId().equals(id)).findAny();
		if (previousIdSound.isPresent()) {
			soundQueue.remove(previousIdSound.get());
			log.debug("removing from soundqueue, remaining size {}", soundQueue.size());
		}
		Optional<SoundConfig> previousNotificationIdSound = notificationQueue.stream().filter(sound -> sound.getId().equals(id)).findAny();
		if (previousNotificationIdSound.isPresent()) {
			notificationQueue.remove(previousNotificationIdSound.get());
			log.debug("removing from notification queue, remaining size {}", notificationQueue.size());
		}

		MediaPlayer player = id2PlayerMap.get(id);
		if (player != null && player.getStatus() != Status.DISPOSED) {
			log.debug("stoping concurrent sound");
			id2PlayerMap.remove(id);
			player.stop();
		}
	}

	public double baseVolumeProperty() {
		return baseVolume;
	}

	public void setBaseVolume(double newValue) {
		log.info("Changing basevolume to {}", newValue);
		this.baseVolume = newValue;
	}
}
