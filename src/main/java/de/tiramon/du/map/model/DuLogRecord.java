package de.tiramon.du.map.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DuLogRecord {
	public String date;
	public long millis;
	public String logger;
	public String level;
	public String clazz;
	public String sequence;
	public String method;
	public String thread;
	public String message;

	public long id;
	static Pattern idPattern = Pattern.compile("(?<uniqueId>\\d+)\\D?");

	public void initId() {
		Matcher matcher = idPattern.matcher(message);
		if (matcher.find()) {
			id = Long.valueOf(matcher.group("uniqueId"));
		} else {
			id = -1;
			// System.out.println(message);
		}
	}
}
