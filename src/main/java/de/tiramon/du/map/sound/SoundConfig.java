package de.tiramon.du.map.sound;

import java.io.File;

import javafx.scene.media.MediaPlayer;

public class SoundConfig {
	String id;
	File url;
	double volume;
	MediaPlayer currentPlayer;

	public SoundConfig(String string, String id2, Long volume2) {
		this.url = new File(string);
		this.id = id2;
		this.volume = volume2;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public File getFile() {
		return url;
	}

	public void setFile(File url) {
		this.url = url;
	}

	public double getVolume() {
		return volume;
	}

	public void setVolume(double volume) {
		this.volume = volume;
	}

	public MediaPlayer getCurrentPlayer() {
		return currentPlayer;
	}

	public void setCurrentPlayer(MediaPlayer currentPlayer) {
		this.currentPlayer = currentPlayer;
	}

}
