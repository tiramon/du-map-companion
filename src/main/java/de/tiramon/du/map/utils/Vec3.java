package de.tiramon.du.map.utils;

public class Vec3 {
	private double x, y, z;

	public Vec3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getZ() {
		return z;
	}

	public void setZ(double z) {
		this.z = z;
	}

	public void add(Vec3 v3) {
		this.x += v3.x;
		this.y += v3.y;
		this.z += v3.z;
	}

	public Vec3 sub(Vec3 v3) {
		this.x -= v3.x;
		this.y -= v3.y;
		this.z -= v3.z;

		return this;
	}

	public void mult(double d) {
		this.x *= d;
		this.y *= d;
		this.z *= d;
	}

	@Override
	public String toString() {
		return toString(",");
	}

	public String toString(String delimiter) {
		return x + delimiter + y + delimiter + z;
	}

	public double dot(Vec3 v) {
		return x * v.x + y * v.y + z * v.z;
	}

	public static Vec3 cross(Vec3 a, Vec3 b) {
		return new Vec3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x);
	}

	@Override
	public Vec3 clone() {
		return new Vec3(x, y, z);
	}

	public Vec3 normalize() {
		return this.divide(this.length());
	}

	public double lengthSq() {
		return Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2);
	}

	public double length() {
		return Math.sqrt(lengthSq());
	}

	public Vec3 divide(double d) {
		return new Vec3(x / d, y / d, z / d);
	}

	public static Vec3 mult(Vec3 vec, double d) {
		return new Vec3(vec.getX() * d, vec.getY() * d, vec.getZ() * d);
	}

	public static double dot(Vec3 v1, Vec3 v2) {
		return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
	}

	public static Vec3 sub(Vec3 v1, Vec3 v2) {
		return new Vec3(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
	}

	public static Vec3 add(Vec3 v1, Vec3 v2) {
		return new Vec3(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(z);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Vec3 other = (Vec3) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		if (Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z))
			return false;
		return true;
	}

	public String toLongString() {
		StringBuilder builder = new StringBuilder();
		builder.append((int) x);
		builder.append(",");
		builder.append((int) y);
		builder.append(",");
		builder.append((int) z);
		return builder.toString();
	}

}
