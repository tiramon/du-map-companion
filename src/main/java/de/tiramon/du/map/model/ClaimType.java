package de.tiramon.du.map.model;

public enum ClaimType {
	CLAIM(1), UPDATE(2), RELEASE(3);

	private long id;

	private ClaimType(long id) {
		this.id = id;
	}

	public long getId() {
		return id;
	}

}
