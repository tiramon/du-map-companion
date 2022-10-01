package de.tiramon.du.map.service;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import bell.oauth.discord.main.OAuthBuilder;
import de.tiramon.du.InstanceProvider;
import de.tiramon.du.map.model.AssetInformation;
import de.tiramon.du.map.model.ClaimType;
import de.tiramon.du.map.model.Ore;

public class DuMapService {
	private String baseUrl = "https://api.dumap.de";

	protected Logger log = LoggerFactory.getLogger(getClass());
	private List<AssetInformation> queue = new CopyOnWriteArrayList<>();

	private Gson gson = InstanceProvider.getGson();
	private OAuthBuilder builder = InstanceProvider.getOAuthBuilder();

	public DuMapService() {
		log.info("DuMap activated");
	}

	public void sendClaimedTile(long celestialId, long tileId, long time, ClaimType claimType) {

		if (builder.getAccess_token() == null) {
			log.info("no access token");
			AssetInformation a = new AssetInformation(celestialId, tileId, time, claimType);

			queue.add(a);
		} else {
			while (!queue.isEmpty()) {
				AssetInformation a = queue.remove(0);
				HttpPost post = createPost(a.getPlanetId(), a.getTileId(), a.getTime(), a.getType());
				sendPost(post);
			}
			HttpPost post = createPost(celestialId, tileId, time, claimType);
			sendPost(post);
		}
	}

	public void sendScan(long celestialId, double lat, double lon, long time, Map<Ore, Long> ores) {
		if (builder.getAccess_token() == null) {
			log.info("no access token");
		} else {
			HttpPost post = createPost(celestialId, lat, lon, time, ores);
			sendPost(post);
		}
	}

	private HttpPost createPost(long celestialId, double lat, double lon, long time, Map<Ore, Long> ores) {
		HttpPost post = new HttpPost(baseUrl + "/scan/automatic");
		post.setHeader("Authorization", "Bearer " + builder.getAccess_token());
		Map<String, Object> request = new HashMap<>();
		request.put("celestialId", celestialId);
		request.put("time", time);
		request.put("lat", lat);
		request.put("lon", lon);
		Map<String, Long> o = new HashMap<>();
		ores.entrySet().forEach(e -> o.put(e.getKey().getName().toLowerCase().replace(" ", "_"), e.getValue()));
		request.put("ores", o);
		post.setEntity(new StringEntity(gson.toJson(request), ContentType.APPLICATION_JSON));
		return post;
	}

	private HttpPost createPost(long celestialId, long tileId, long time, ClaimType claimType) {
		HttpPost post = new HttpPost(baseUrl + "/asset");
		post.setHeader("Authorization", "Bearer " + builder.getAccess_token());
		Map<String, Object> request = new HashMap<>();
		request.put("planetId", celestialId);
		request.put("tileId", tileId);
		request.put("time", time);
		request.put("type", claimType.toString());
		post.setEntity(new StringEntity(gson.toJson(request), ContentType.APPLICATION_JSON));
		return post;
	}

	private void sendPost(HttpPost post) {
		try (CloseableHttpClient client = HttpClients.createDefault()) {

			HttpResponse response = client.execute(post);
			int statusCode = response.getStatusLine().getStatusCode();
			log.info("{} status {}", post.getURI(), statusCode);
			HttpEntity respEntity = response.getEntity();
			Header encodingHeader = respEntity.getContentEncoding();

			// you need to know the encoding to parse correctly
			Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8 : Charsets.toCharset(encodingHeader.getValue());

			// use org.apache.http.util.EntityUtils to read json as string
			String json = EntityUtils.toString(respEntity, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
