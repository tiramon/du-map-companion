package de.tiramon.du.map.model;

public class AssetInformation {
	public AssetInformation(long celestialId, long tileId2, long time2) {
		this.planetId = celestialId;
		this.tileId = tileId2;
		this.time = time2;
	}

	public long planetId;
	public long tileId;
	public long time;
}
