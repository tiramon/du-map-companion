package de.tiramon.du;

public enum Feature {
	SCANNER, SOUND, ASSET, MARKET;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
