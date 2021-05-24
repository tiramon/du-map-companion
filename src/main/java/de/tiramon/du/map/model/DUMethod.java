package de.tiramon.du.map.model;

public enum DUMethod {
	//@formatter:off
	TERRITORYSCANNER_STATUSCHANGE("NQ::Game::AssetEventsElementHub::soundActionPostEvent"),
	TERRITORYSCANNER_POSITION("NQ::Game::TerritoryScan::initHFSM::<lambda>::operator ()"),
	TERRITORYSCANNER_RESULT("NQ::Game::TerritoryScan::getResults"),
	ASSET_CLAIM("NQ::Game::TerritoryManager::onTerritoryClaimed"),
	ASSET_RIGHTS("NQ::RDMS::RightsCache<class NQ::Game::Territory>::requestRights::<lambda>::operator ()");
	//@formatter:on

	private DUMethod(String dumethod) {
		this.dumethodString = dumethod;
	}

	private String dumethodString;

	public static DUMethod get(String value) {
		DUMethod[] a = values();
		for (int i = 0; i < a.length; i++) {
			if (a[i].dumethodString.equals(value)) {
				return a[i];
			}
		}
		return null;
	}
}
