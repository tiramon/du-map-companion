package de.tiramon.du.map.thread;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;

import de.tiramon.du.map.DuMapDialog;
import de.tiramon.du.map.InstanceProvider;
import de.tiramon.du.map.model.DuLogRecord;
import de.tiramon.du.map.service.Service;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableValue;

public class FileReader implements Runnable {
	protected Logger log = LoggerFactory.getLogger(getClass());

	private Service handleService = InstanceProvider.getService();
	private XStream xstream;
	private ReadOnlyObjectProperty<Path> currentLogFileProperty;

	// set to true when application is about to close so reading is stopped and no
	// exception is thrown
	private boolean shutdown = false;

	public FileReader(ReadOnlyObjectProperty<Path> currentLogfileProperty) {
		this.currentLogFileProperty = currentLogfileProperty;
	}

	@Override
	public void run() {
		log.info("FileReader started");
		setupXStream();
		currentLogFileProperty.addListener((ObservableValue<? extends Path> observable, Path oldValue, Path newValue) -> {
			log.info("new log file {}", newValue);
			readFile(currentLogFileProperty.get());
		});
		log.info("listener added");

	}

	private void setupXStream() {
		xstream = new XStream();
		// clear out existing permissions and set own ones
		xstream.addPermission(NoTypePermission.NONE);
		// allow some basics
		xstream.addPermission(NullPermission.NULL);
		xstream.addPermission(PrimitiveTypePermission.PRIMITIVES);
		xstream.allowTypeHierarchy(Collection.class);
		// allow any type from the same package
		xstream.allowTypesByWildcard(new String[] { "de.tiramon.du.**.model.**" });

		// specific mapping, record also exists as java.util, so mapping to a unique
		// class name
		xstream.alias("record", DuLogRecord.class);
		// class is a reserved word in java so we need to map it to something that is
		// not reserved
		xstream.aliasAttribute(DuLogRecord.class, "clazz", "class");
	}

	public void onShutdown() {
		shutdown = true;
	}

	private void readFile(Path path) {
		log.info("start reading logfile {}", path);
		if (path.getFileName().toString().endsWith(".lnk")) {
			return;
		}
		if (!path.toFile().isFile()) {
			return;
		}
		String line;
		List<String> lineBuffer = new ArrayList<>();
		long start = System.currentTimeMillis();
		try (BufferedReader br = Files.newBufferedReader(path)) {
			while (true) {
				if (shutdown) {
					return;
				}

				if (!currentLogFileProperty.get().equals(path)) {
					log.info("stoping reading old logfile");
					return;
				}
				line = br.readLine();

				if (line == null) {
					DuMapDialog.init = true;
					Thread.sleep(1000);
				} else {
					line = line.trim();
					// if end of record entry is reached, parse and process
					if (line.equals("</record>")) {
						lineBuffer.add(line);
						DuLogRecord record = mapToRecord(lineBuffer);
						record.initId();
						if (record.id == 2772531619L) {
							this.handleService.handleAsset(record);
						} else if (record.id == 384688231L) {
							this.handleService.handleScanStatusChange(record);
						} else if (record.id == 848570271L) {
							this.handleService.handleScanOre(record);
						} else if (record.id == 411172400L) {
							this.handleService.handleScanPosition(record);
						} else if (record.id == 2339009767L) {
							this.handleService.handlePlanet(record);
						} else if (record.id == 1400672326L) {
							this.handleService.handleUser(record);
						}
						// addAvgReadPerRecord(System.currentTimeMillis() - start);
						// publish record for further processing

						start = System.currentTimeMillis();
						// publishRecord(record);
						// addAvgProcessPerRecord(System.currentTimeMillis() - start);

						// clear buffer to start new entry
						lineBuffer.clear();

						start = System.currentTimeMillis();

					} else {
						// if not end of record add to buffer for later parsing
						lineBuffer.add(line);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private DuLogRecord mapToRecord(List<String> lineBuffer) throws IOException, ClassNotFoundException {
		// need to add a wrapper to be able to parse the element correctly
		lineBuffer.add(0, "<wrapper>");
		lineBuffer.add("</wrapper>");
		String str = lineBuffer.stream().collect(Collectors.joining());
		ObjectInputStream in = xstream.createObjectInputStream(new ByteArrayInputStream(str.getBytes()));
		DuLogRecord record = (DuLogRecord) in.readObject();
		return record;
	}

	public void stop() {
		shutdown = true;
	}
}
