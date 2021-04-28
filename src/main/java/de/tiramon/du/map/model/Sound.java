package de.tiramon.du.map.model;

public class Sound {
	private String title;
	private String resourceFileName;

	public Sound(String title, String resourceFileName) {
		this.title = title;
		this.resourceFileName = resourceFileName;
	}

	public String getTitle() {
		return title;
	}

	public String getResourceFileName() {
		return resourceFileName;
	}

	@Override
	public String toString() {
		return title;
	}
}
