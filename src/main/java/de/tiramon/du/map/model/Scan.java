package de.tiramon.du.map.model;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * Model for a territory scanner scan
 */
public class Scan {
	private long timestamp;
	private String pos;
	private ObservableMap<Ore, Long> oreMap = FXCollections.observableHashMap();

	private int planetId;
	private double lat;
	private double lon;

	private static Pattern posPattern = Pattern.compile("0,(?<planetId>\\d+),(?<lat>-?\\d+\\.\\d+),(?<lon>-?\\d+\\.\\d+),");

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public Scan(String pos, long timestamp) {
		this.pos = pos;
		this.timestamp = timestamp;
		Matcher matcher = posPattern.matcher(pos);
		matcher.find();

		planetId = Integer.parseInt(matcher.group("planetId"));
		lat = Double.parseDouble(matcher.group("lat"));
		lon = Double.parseDouble(matcher.group("lon"));
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	public int getPlanet() {
		return planetId;
	}

	public void add(Ore ore, long amount) {
		oreMap.put(ore, amount);
	}

	static Comparator<Entry<Ore, Long>> orecomp = (a, b) -> {
		int t = a.getKey().getTier() - b.getKey().getTier();
		if (t != 0) {
			return t;
		}

		return a.getKey().getName().compareTo(b.getKey().getName());
	};

	public Map<Ore, Long> getOres() {
		return oreMap;
	}

	@Override
	public String toString() {
		return pos + ";" + timestamp + ";" + sdf.format(timestamp) + ";" + (oreMap.isEmpty() ? "waiting for data" : oreMap.entrySet().stream().sorted(orecomp).map(entry -> entry.getKey().getName() + "=" + entry.getValue()).collect(Collectors.joining(";")));
	}

	public String toSQL() {
		String oreColumns = oreMap.entrySet().stream().sorted(orecomp).map(Entry::getKey).map(Ore::getName).map(String::toLowerCase).map(s -> s.replace(" ", "_")).collect(Collectors.joining(","));
		String oreValues = oreMap.entrySet().stream().sorted(orecomp).map(Entry::getValue).map(String::valueOf).collect(Collectors.joining(","));
		return "INSERT INTO scan (celestial_id, tile_id, owner_id, timestamp, " + oreColumns + ") VALUES ((SELECT celestial_id FROM celestial_body WHERE du_entity_id = " + planetId + "), (SELECT tile_id FROM gp_lat_lon where gp_size = (SELECT gp FROM celestial_body WHERE du_entity_id = 2) order by (abs(lon-" + lon + ")+abs(lat-" + lat + ")) ASC LIMIT 1), 1,'" + sdf.format(timestamp) + "', " + oreValues + ");";
	}

	public long getTimestamp() {
		return timestamp;
	}

	public String getPos() {
		return pos;
	}

}
