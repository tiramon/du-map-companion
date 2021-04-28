package de.tiramon.du.map.model;

public class User {

	private Long playerId;
	private Long communityId;
	private String playerName;

	public User(Long playerId, Long communityId, String playerName) {
		this.playerId = playerId;
		this.communityId = communityId;
		this.playerName = playerName;
	}

	public Long getPlayerId() {
		return playerId;
	}

	public Long getCommunityId() {
		return communityId;
	}

	public String getPlayerName() {
		return playerName;
	}
}
