package de.tiramon.du.map.thread;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tiramon.du.map.InstanceProvider;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class NewFileWatcher implements Runnable {
	protected Logger log = LoggerFactory.getLogger(getClass());

	private SimpleObjectProperty<Path> currentLogFile = new SimpleObjectProperty<>(null);
	private Path logFolder;

	private WatchKey folderKey;

	private List<Path> queue;

	public NewFileWatcher(List<Path> queue) {
		this.queue = queue;

		Path homepath = Paths.get(System.getProperty("user.home"));
		logFolder = Paths.get(homepath.toString(), "\\AppData\\Local\\NQ\\DualUniverse\\log");

		boolean readAll = Boolean.parseBoolean((String) InstanceProvider.getProperties().getOrDefault("readAll", "false"));
		if (readAll) {
			try {
				List<Path> list = Files.list(logFolder).sorted((a, b) -> Long.compare(a.toFile().lastModified(), b.toFile().lastModified())).collect(Collectors.toList());
				list.remove(list.size() - 1);
				list.forEach(p -> this.queue.add(p));
				log.info("added {} to backlog", list.size());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		currentLogFile.addListener(new ChangeListener<Path>() {
			@Override
			public void changed(ObservableValue<? extends Path> observable, Path oldValue, Path newValue) {
				synchronized (queue) {
					queue.add(newValue);
				}

			}
		});
	}

	@Override
	public void run() {
		log.info("LogFileWatcher started");

		try {
			Thread.sleep(500);

			WatchService watchService = FileSystems.getDefault().newWatchService();
			folderKey = logFolder.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
			log.info("registered DU log folder {}", logFolder);
			Path logFile = getNewestLogfile();

			if (logFile != null) {
				log.info("Setting newest log file as current log file: {}", logFile);
				currentLogFile.set(logFile);
			}

			WatchKey key = null;
			while ((key = watchService.take()) != null) {
				for (WatchEvent<?> event : key.pollEvents()) {
					// process
					Path affectedFile = (Path) event.context();
					// System.out.println("Event kind:" + event.kind() + ". File affected: " +
					// event.context() + ".");

					Path newLogFile = Paths.get(logFolder.toString(), affectedFile.toString());
					log.info("new file {}", newLogFile);
					currentLogFile.set(newLogFile);
				}
				key.reset();
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private Path getNewestLogfile() throws IOException {
		return Files.list(logFolder).max((p1, p2) -> {
			try {
				FileTime t1 = Files.getLastModifiedTime(p1);
				FileTime t2 = Files.getLastModifiedTime(p2);
				return t1.compareTo(t2);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return -1;
			}
		}).orElseGet(null);
	}

	public ReadOnlyObjectProperty<Path> getCurrentLogFileProperty() {
		return currentLogFile;
	}

	public void stop() {
		folderKey.cancel();
	}
}
