package de.tiramon.du.map.model;

import com.google.gson.annotations.SerializedName;

import de.tiramon.du.map.utils.Vec3;

public class Planet {
	@SerializedName("GM")
	long gm;

	long bodyId;

	Vec3 center;
	String name;
	long planetarySystemId;
	long radius;

	Vec3 logCoordCenter;

	public Vec3 getLogCoordCenter() {
		if (logCoordCenter == null) {
			int numBits = Long.SIZE - Long.numberOfLeadingZeros(radius);
			int modifier = (int) Math.pow(2, numBits) - 1;
			logCoordCenter = Vec3.sub(center, new Vec3(modifier, modifier, modifier));
		}
		return logCoordCenter;
	}

	int log2(int value) {
		return Integer.SIZE - Integer.numberOfLeadingZeros(value);
	}

	public long getGm() {
		return gm;
	}

	public long getBodyId() {
		return bodyId;
	}

	public Vec3 getCenter() {
		return center;
	}

	public String getName() {
		return name;
	}

	public long getPlanetarySystemId() {
		return planetarySystemId;
	}

	public long getRadius() {
		return radius;
	}

}
