package de.tiramon.du;

import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Application;

public class DuCompanion {
	protected static Logger log = LoggerFactory.getLogger(DuCompanion.class);

	public static void main(String[] args) throws ParseException {
		String javaversion = System.getProperty("java.version");
		log.info("Running Java version: {}", javaversion);
		InstanceProvider.init();
		Application.launch(DuDialog.class, args);

	}
}
