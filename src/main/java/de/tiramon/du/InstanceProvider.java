package de.tiramon.du;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import bell.oauth.discord.domain.User;
import bell.oauth.discord.main.OAuthBuilder;
import de.tiramon.du.service.Service;
import de.tiramon.du.sound.service.SoundService;
import de.tiramon.github.update.service.UpdateService;

public class InstanceProvider {
	protected static Logger log = LoggerFactory.getLogger(InstanceProvider.class);

	private static Gson gson = new Gson();
	private static Properties properties = null;
	private static OAuthBuilder oauthbuilder = null;
	private static Service service = null;
	private static SoundService soundService = null;
	private static UpdateService updateService = null;
	private static Map<Feature, String> defaultFeatureState = new HashMap<>();

	static {
		defaultFeatureState.put(Feature.MARKET, "false");
		defaultFeatureState.put(Feature.SOUND, "false");
		defaultFeatureState.put(Feature.ASSET, "true");
		defaultFeatureState.put(Feature.SCANNER, "true");
	}

	static void init() {
		properties = initProperties();
		for (Feature feature : Feature.values()) {
			log.info("Feature {} is {}", feature.toString(), isFeatureActive(feature) ? "active" : "inactive");
		}
		updateService = new UpdateService("https://api.github.com/repos/tiramon/du-map-companion/releases/latest");
		oauthbuilder = oauthBuilder();
		if (isFeatureActive(Feature.SOUND) || isFeatureActive(Feature.SCANNER)) {
			soundService = new SoundService(Boolean.valueOf(properties.getProperty("sound.framework.enabled", "false")));
		}
		service = new Service();
	}

	private static Properties initProperties() {
		Properties properties = new Properties();
		File file = new File("application.properties");
		if (file.exists() && file.canRead()) {
			log.info("Found properties file");
			try (FileInputStream propertiesInput = new FileInputStream(file)) {
				properties.load(propertiesInput);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return properties;
	}

	public static boolean isFeatureActive(Feature feature) {
		String propertiesValue = (String) properties.getOrDefault("feature." + feature.toString() + ".active", defaultFeatureState.get(feature));
		return Boolean.valueOf(propertiesValue);

	}

	private static OAuthBuilder oauthBuilder() {
		OAuthBuilder builder = new OAuthBuilder("780864362234511400", "Tk1Ni6x6wm239aN2juHh3o90glPusCqB").setScopes(new String[] { "identify" }).setRedirectURI("http://localhost:4201/");
		String accessToken = InstanceProvider.getProperties().getProperty("access");

		if (!StringUtils.isEmpty(accessToken)) {
			log.info("Found access token '{}' in properties", accessToken);

			builder.setAccess_token(accessToken);
			try {
				User user = builder.getUser();
			} catch (JSONException e) {
				builder.setAccess_token(null);
				log.info("Could not use autologin '{}' {}", accessToken, e.getMessage());
			}

		}
		return builder;
	}

	public static OAuthBuilder getOAuthBuilder() {
		return oauthbuilder;
	}

	public static Properties getProperties() {
		return properties;
	}

	public static Gson getGson() {
		return gson;
	}

	public static Service getService() {
		return service;
	}

	public static SoundService getSoundService() {
		return soundService;
	}

	public static UpdateService getUpdateService() {
		return updateService;
	}
}
