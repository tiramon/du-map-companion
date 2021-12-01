package de.tiramon.du.map.model;

import de.tiramon.du.tools.model.DUMethod;

public enum DUMethodsMap implements DUMethod {
	//@formatter:off
	TERRITORYSCANNER_STATUSCHANGE("NQ::Game::AssetEventsElementHub::soundActionPostEvent"),
	TERRITORYSCANNER_STATUSSTART("NQ::Game::TerritoryScan::launchScan"),
	TERRITORYSCANNER_POSITION("NQ::Game::TerritoryScan::initHFSM::<lambda>::operator ()"),
	TERRITORYSCANNER_RESULT("NQ::Game::TerritoryScan::getResults"),
	TERRITORYSCANNER_RESET("NQ::Coroutine::Handle::kill"),
	ASSET_CLAIM("NQ::Game::TerritoryManager::onTerritoryClaimed"),
	ASSET_RIGHTS("NQ::RDMS::RightsCache<class NQ::Game::Territory>::requestRights::<lambda>::operator ()"),
	ASSET_RELEASED("NQ::Game::TerritoryManager::onTerritoryReleased"),
	LOG_INFO("NQ::DP::System::logInfo");
	//@formatter:on

	private DUMethodsMap(String dumethod) {
		this.dumethodString = dumethod;
	}

	private String dumethodString;

	@Override
	public String getMethodString() {
		return dumethodString;
	}
}
