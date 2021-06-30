package de.tiramon.du.map.update;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tiramon.du.map.InstanceProvider;

public class UpdateService {
	private SemanticVersion currentVersion = null;
	private SemanticVersion githubVersion = null;
	private GitHubReleaseInformation gitHubReleaseInformation;

	protected Logger log = LoggerFactory.getLogger(getClass());
	private String latestReleaseApiUrl = "https://api.github.com/repos/tiramon/du-map-companion/releases/latest";

	public UpdateService() {
		try {
			currentVersion = getSemanticVersionIntern();
			log.info("Installed Version: {}", currentVersion);
			githubVersion = getNewestGithubVersion();
			log.info("Newest Version: {}", githubVersion);
		} catch (ParseException e) {
		}
	}

	public boolean isUpdateAvailable() {
		if (githubVersion != null) {
			return githubVersion.isUpdateFor(currentVersion);
		} else {
			return false;
		}
	}

	public SemanticVersion getSemanticVersion() {
		return currentVersion;
	}

	public SemanticVersion getSemanticGithubVersion() {
		return githubVersion;
	}

	private SemanticVersion getSemanticVersionIntern() throws ParseException {
		return new SemanticVersion(UpdateService.class);
	}

	private SemanticVersion getNewestGithubVersion() throws ParseException {
		HttpGet get = new HttpGet(latestReleaseApiUrl);
		try {
			String jsonString = sendGet(get);
			gitHubReleaseInformation = InstanceProvider.getGson().fromJson(jsonString, GitHubReleaseInformation.class);
			SemanticVersion githubVersion = new SemanticVersion(gitHubReleaseInformation.getTagName().substring(1));

			return githubVersion;
		} catch (IOException e) {
			return null;
		}
	}

	private String sendGet(HttpGet get) throws IOException {
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpResponse response = client.execute(get);
			int statusCode = response.getStatusLine().getStatusCode();
			log.info("{} status {}", get.getURI(), statusCode);
			HttpEntity respEntity = response.getEntity();
			Header encodingHeader = respEntity.getContentEncoding();

			// you need to know the encoding to parse correctly
			Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8 : Charsets.toCharset(encodingHeader.getValue());

			// use org.apache.http.util.EntityUtils to read json as string
			String json = EntityUtils.toString(respEntity, StandardCharsets.UTF_8);
			return json;
		} catch (IOException e) {
			throw e;
		}
	}

	public GitHubReleaseInformation getGithubReleaseInformation() {
		return gitHubReleaseInformation;
	}
}
