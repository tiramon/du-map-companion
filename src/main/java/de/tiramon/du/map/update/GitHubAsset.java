package de.tiramon.du.map.update;

import com.google.gson.annotations.SerializedName;

public class GitHubAsset {
	String name;
	long size;
	@SerializedName("browser_download_url")
	String browserDownloadUrl;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getBrowserDownloadUrl() {
		return browserDownloadUrl;
	}

	public void setBrowserDownloadUrl(String browserDownloadUrl) {
		this.browserDownloadUrl = browserDownloadUrl;
	}

}
