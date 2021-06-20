package de.tiramon.du.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;

public class DuMapCompanion {
	protected static Logger log = LoggerFactory.getLogger(DuMapCompanion.class);

	public static void main(String[] args) {
		String javaversion = System.getProperty("java.version");
		log.info("Running Java version: {}", javaversion);
		InstanceProvider.init();
		Application.launch(DuMapDialog.class, args);

	}
}
