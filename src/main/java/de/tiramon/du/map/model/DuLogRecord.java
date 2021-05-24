package de.tiramon.du.map.model;

/**
 * Model for the xml structure used by NQ in the DualUniverse log files
 */
public class DuLogRecord {
	public String date;
	public long millis;
	public String logger;
	public String level;
	public String clazz;
	public String sequence;
	public DUMethod method;
	public String thread;
	public String message;

	@Override
	public String toString() {
		return this.clazz + " " + this.method + " " + this.millis + " " + this.message;
	}
}
