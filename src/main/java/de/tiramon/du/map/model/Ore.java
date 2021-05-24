package de.tiramon.du.map.model;

import java.util.Arrays;

public enum Ore {
	// @formatter:off
	BAUXITE    ("Bauxite"     , "Aluminiu",  1, 1),
	COAL       ("Coal"        , "CarbonOr",  2, 1),
	HEMATITE   ("Hematite"    , "IronOre",   3, 1),
	QUARTZ     ("Quartz"      , "SiliconO",  4, 1),

	CHROMITE   ("Chromite"    , "Chromium",  5, 2),
	LIMESTONE  ("Limestone"   , "CalciumO",  6, 2),
	MALACHITE  ("Malachite"   , "CopperOr",  7, 2),
	NATRON     ("Natron"      , "SodiumOr",  8, 2),

	ACANTHITE  ("Acanthite"   , "SilverOr",  9, 3),
	GARNIERITE ("Garnierite"  , "NickelOr", 10, 3),
	PETALITE   ("Petalite"    , "LithiumO", 11, 3),
	PYRITE     ("Pyrite"      , "SulfurOr", 12, 3),

	COBALTITE  ("Cobaltite"   , "CobaltOr", 13, 4),
	CRYOLITE   ("Cryolite"    , "Fluorine", 14, 4),
	KOLBECKITE ("Kolbeckite"  , "Scandium", 15, 4),
	GOLDNUGGETS("Gold nuggets", "GoldOre",  16, 4),

	COLUMBITE  ("Columbite"   , "NiobiumO", 17, 5),
	RHODONITE  ("Rhodonite"   , "Manganes", 18, 5),
	THORAMINE  ("Thoramine"   , "Thoramin", 19, 5),
	TITANIUM   ("Illmenite"   , "Titanium", 20, 5),
	VANADINITE ("Vanadinite"  , "Vanadium", 21, 5);
	//@formatter:on

	private String name;
	private String logOreName;
	private int id;
	private int tier;

	private Ore(String name, String logOreName, int id, int tier) {
		this.name = name;
		this.logOreName = logOreName;
		this.id = id;
		this.tier = tier;
	}

	public String getName() {
		return name;
	}

	public String getLogOreName() {
		return logOreName;
	}

	public int getId() {
		return id;
	}

	public static Ore byLogOreName(String needle) {
		return Arrays.stream(values()).filter(ore -> ore.logOreName != null && ore.logOreName.equals(needle)).findFirst().orElseThrow(() -> new RuntimeException("No Ore found '" + needle + "'"));
	}

	public static Ore byName(String needle) {
		return Arrays.stream(values()).filter(ore -> ore.name.equals(needle)).findFirst().orElse(null);
	}

	public static Ore byId(int needle) {
		return Arrays.stream(values()).filter(ore -> ore.id == needle).findFirst().orElse(null);
	}

	public int getTier() {
		return tier;
	}
}
