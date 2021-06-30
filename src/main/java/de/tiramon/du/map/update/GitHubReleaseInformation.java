package de.tiramon.du.map.update;

import java.util.Date;

import com.google.gson.annotations.SerializedName;

public class GitHubReleaseInformation {
	@SerializedName("html_url")
	String htmlUrl;
	@SerializedName("tag_name")
	String tagName;
	@SerializedName("published_at")
	Date publishedAt;
	GitHubAsset[] assets;

	public String getHtmlUrl() {
		return htmlUrl;
	}

	public void setHtmlUrl(String htmlUrl) {
		this.htmlUrl = htmlUrl;
	}

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public Date getPublishedAt() {
		return publishedAt;
	}

	public void setPublishedAt(Date publishedAt) {
		this.publishedAt = publishedAt;
	}

	public GitHubAsset[] getAssets() {
		return assets;
	}

	public void setAssets(GitHubAsset[] assets) {
		this.assets = assets;
	}

}
