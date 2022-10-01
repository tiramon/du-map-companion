package de.tiramon.du.market.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bell.oauth.discord.main.OAuthBuilder;
import de.tiramon.du.InstanceProvider;
import de.tiramon.du.market.api.MarketApi;
import de.tiramon.du.market.api.OrderApi;
import de.tiramon.du.market.client.ApiClient;
import de.tiramon.du.market.client.ApiException;
import de.tiramon.du.market.model.Market;
import de.tiramon.du.market.model.Order;
import de.tiramon.du.market.model.Position;
import de.tiramon.du.tools.model.DuLogRecord;
import de.tiramon.github.update.service.UpdateService;
import javafx.application.Platform;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;

public class MarketService {
	protected Logger log = LoggerFactory.getLogger(getClass());
	Executor executor = Executors.newCachedThreadPool();

	private ApiClient apiClient;
	private OrderApi orderApi;
	private MarketApi marketApi;

	private Pattern marketInfo = Pattern.compile(
			"MarketInfo:\\[marketId = (?<marketid>\\d+), relativeLocation = RelativeLocation:\\[constructId = (?<constructid>\\d+), position = Vec3:\\[(?<relx>-?\\d+\\.\\d+), (?<rely>-?\\d+\\.\\d+), (?<relz>-?\\d+\\.\\d+)\\], rotation = Quat:\\[(?<rot1>-?\\d+\\.\\d+), (?<rot2>-?\\d+\\.\\d+), (?<rot3>-?\\d+\\.\\d+), (?<rot4>-?\\d+\\.\\d+)\\]\\], position = Vec3:\\[(?<posx>-?\\d+\\.\\d+), (?<posy>-?\\d+\\.\\d+), (?<posz>-?\\d+\\.\\d+)\\], parentConstruct = (?<parentconstructid>\\d+), name = (?<name>.+?), creatorId = EntityId:\\[playerId = (?<creatorPlayerId>\\d+), organizationId = (?<creatorOrgId>\\d+)\\], creatorName = (?<creatorName>.+?), creationDate = @\\((?<creationDate>\\d+)\\) \\d+-\\d+-\\d+ \\d+:\\d+:\\d+, capacity = (?<capacity>\\d+), valueTax = (?<valueTax>\\d+(?:\\.\\d+)?), dailyStorageFee = (?<dailyStorageFee>\\d+(?:\\.\\d+)?), orderFee = (?<orderFee>\\d+(?:\\.\\d+)?), allowedItemTypes = \\[(\\d+, )*\\]updateCooldown = (?<updateCooldown>\\d+)\\]");
	private Pattern marketstorageslotPattern = Pattern.compile("MarketStorageSlotEx:\\[marketId = (?<marketId>\\d+), position = (?<position>\\d+), itemAndQuantity = ItemAndQuantity:\\[item = ItemInfo:\\[type = (?<itemtype>\\d+), id = 0, owner = EntityId:\\[playerId = 0, organizationId = 0\\], properties = \\[\\], quantity = (?<quantity>\\d+)\\], purchased = (?<purchased>true|false)\\]");
	private Pattern marketOrderPattern = Pattern.compile("MarketOrder:\\[marketId = (?<marketId>\\d+), orderId = (?<orderId>\\d+), itemType = (?<itemtype>\\d+), buyQuantity = (?<quantity>-?\\d+), expirationDate = @\\((?<expirationTimestamp>\\d+)\\) \\d+-\\d+-\\d+ \\d+:\\d+:\\d+, updateDate = @\\((?<updateTimestamp>\\d+)\\) \\d+-\\d+-\\d+ \\d+:\\d+:\\d+, unitPrice = Currency:\\[amount = (?<price>\\d+(?:\\.\\d+)?)\\]\\]");
	private Pattern marketOrderCancelPattern = Pattern.compile("onOrderCanceled: (?<marketId>\\d+):(?<orderId>\\d+)");

	private OAuthBuilder builder = InstanceProvider.getOAuthBuilder();
	private UpdateService updateService = InstanceProvider.getUpdateService();

	// private Map<Long, Item> id2Item;
	private Map<Long, Market> id2market = new HashMap<>();

	LongProperty outstandingRequest = new SimpleLongProperty(0);

	public MarketService() {
		apiClient = new ApiClient(HttpClients.createDefault());
		// apiClient.setBasePath("http://localhost:8080");
		apiClient.setBasePath("http://api.dumap.de:8151");
		apiClient.setBearerToken(builder.getAccess_token());
		apiClient.setUserAgent("DuCompanion " + updateService.getSemanticVersion().toString());
		orderApi = new OrderApi(apiClient);
		marketApi = new MarketApi(apiClient);
	}

	public void handleMarketList(DuLogRecord record) throws ApiException {
		if (!id2market.isEmpty()) {
			return;
		}

		Matcher matcher = marketInfo.matcher(record.message);
		final List<Market> marketList = new ArrayList<>();
		while (matcher.find()) {
			long marketId = Long.valueOf(matcher.group("marketid"));
			String marketName = matcher.group("name");
			double x = Double.valueOf(matcher.group("posx"));
			double y = Double.valueOf(matcher.group("posy"));
			double z = Double.valueOf(matcher.group("posz"));

			double valueTax = Double.valueOf(matcher.group("valueTax"));
			Long capacity = Long.valueOf(matcher.group("capacity"));
			double dailyStorageFee = Double.valueOf(matcher.group("dailyStorageFee"));
			double orderFee = Double.valueOf(matcher.group("orderFee"));
			long parentConstruct = Long.valueOf(matcher.group("parentconstructid"));
			// Vec3 pos = new Vec3(x, y, z);
			// Planet planet = nqService.getPlanetByConstructId(parentConstruct);

			// if (planet == null) {
			// log.info("No Planet found for market {} with parentconstructid {}", marketName, parentConstruct);
			// }
			Market m = new Market().marketId(marketId).name(marketName).position(new Position().x(x).y(y).z(z)).parentConstructId(parentConstruct).capacity(capacity).orderFee(orderFee).valueTax(valueTax).dailyStorageFee(dailyStorageFee);
			m = id2market.putIfAbsent(marketId, m);
			if (m == null) {
				log.debug("registered market {} {}", marketId, marketName);
			} else {
				log.debug("market {} {} was already known to the system", marketId, marketName);
			}
		}
		marketList.addAll(id2market.values().stream().sorted((a, b) -> a.getName().compareTo(b.getName())).collect(Collectors.toList()));
		asyncRequest(() -> {
			try {
				marketApi.createMarkets(marketList);
				decrementOutstandingRequests();
			} catch (ApiException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
	}

	public void incrementOutstandingRequests() {
		Platform.runLater(() -> outstandingRequest.set(outstandingRequest.get() + 1));
	}

	public void decrementOutstandingRequests() {
		Platform.runLater(() -> outstandingRequest.set(outstandingRequest.get() - 1));
	}

	public void handleMarketItemOrders(DuLogRecord record) throws ApiException {
		Matcher matcher = marketOrderPattern.matcher(record.message);
		List<Order> orderList = new ArrayList<>();
		Long itemType = null;
		while (matcher.find()) {
			long orderId = Long.valueOf(matcher.group("orderId"));
			long marketId = Long.valueOf(matcher.group("marketId"));
			// Market market = id2market.get(marketId);
			long itemId = Long.valueOf(matcher.group("itemtype"));
			itemType = itemId;
			long amount = Long.valueOf(matcher.group("quantity"));
			long expirationTimestamp = Long.valueOf(matcher.group("expirationTimestamp"));
			double price = Double.valueOf(matcher.group("price")) / 100d;

			// if (market == null) {
			// throw new RuntimeException("Market is null " + marketId + " " + matcher.group());
			// }

			Order orderDto = new Order().orderId(orderId).marketId(marketId).itemType(itemId).buyQuantity(amount).expirationDate(expirationTimestamp).unitPrice(price).timestamp(record.millis);
			orderList.add(orderDto);
		}
		log.info("got market informations for item {}", itemType);
		asyncRequest(() -> {
			try {
				orderApi.createAndUpdateOrders(orderList);
				decrementOutstandingRequests();
			} catch (ApiException e) {
				e.printStackTrace();
			}
		});
	}

	public void asyncRequest(Runnable runable) {
		incrementOutstandingRequests();
		executor.execute(runable);
	}

	public void handleMarketOrderCanceled(DuLogRecord record) {
		// TODO Auto-generated method stub

	}

	public void handleMarketPersonal(DuLogRecord record) {
		// TODO Auto-generated method stub

	}

	public void handleMarketSlot(DuLogRecord record) {
		// TODO Auto-generated method stub

	}

	public LongProperty outstandingRequestsProperty() {
		return outstandingRequest;
	}

}
