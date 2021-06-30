package de.tiramon.du.map.model;

/**
 * Simple model containing planet id, tile id and timestamp it was found
 */
public class AssetInformation {
	private long planetId;
	private long tileId;
	private long time;
	private ClaimType type;

	public AssetInformation(long celestialId, long tileId2, long time2, ClaimType type) {
		this.planetId = celestialId;
		this.tileId = tileId2;
		this.time = time2;
		this.type = type;
	}

	public long getPlanetId() {
		return planetId;
	}

	public void setPlanetId(long planetId) {
		this.planetId = planetId;
	}

	public long getTileId() {
		return tileId;
	}

	public void setTileId(long tileId) {
		this.tileId = tileId;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public ClaimType getType() {
		return type;
	}

	public void setType(ClaimType type) {
		this.type = type;
	}
}
