package no.rutebanken.anshar.subscription;


import com.hazelcast.core.IMap;
import no.rutebanken.anshar.messages.EstimatedTimetables;
import no.rutebanken.anshar.messages.ProductionTimetables;
import no.rutebanken.anshar.messages.Situations;
import no.rutebanken.anshar.messages.VehicleActivities;
import no.rutebanken.anshar.routes.health.HealthManager;
import no.rutebanken.anshar.routes.siri.SiriObjectFactory;
import no.rutebanken.anshar.subscription.enums.SubscriptionMode;
import no.rutebanken.anshar.subscription.models.Subscription;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SubscriptionManager {

    final int HEALTHCHECK_INTERVAL_FACTOR = 5;
    private Logger logger = LoggerFactory.getLogger(SubscriptionManager.class);

//    @Autowired
//    @Qualifier("getSubscriptionsMap")
//    private IMap<String, SubscriptionSetup> subscriptions;

    @Autowired
    SubscriptionRepository repository;

    @Autowired
    @Qualifier("getLastActivityMap")
    private IMap<String, java.time.Instant> lastActivity;

    @Autowired
    @Qualifier("getDataReceivedMap")
    private IMap<String, java.time.Instant> dataReceived;

    @Autowired
    @Qualifier("getActivatedTimestampMap")
    private IMap<String, java.time.Instant> activatedTimestamp;

    @Value("${anshar.environment}")
    private String environment;

    @Autowired
    private IMap<String, Integer> hitcount;

    @Autowired
    private IMap<String, BigInteger> objectCounter;

    @Autowired
    private SiriObjectFactory siriObjectFactory;

    @Autowired
    private HealthManager healthManager;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Autowired
    private Situations sx;
    @Autowired
    private EstimatedTimetables et;
    @Autowired
    private ProductionTimetables pt;
    @Autowired
    private VehicleActivities vm;

    @Autowired
    @Qualifier("getSituationChangesMap")
    private IMap<String, Set<String>> sxChanges;

    @Autowired
    @Qualifier("getEstimatedTimetableChangesMap")
    private IMap<String, Set<String>> etChanges;

    @Autowired
    @Qualifier("getProductionTimetableChangesMap")
    private IMap<String, Set<String>> ptChanges;

    @Autowired
    @Qualifier("getVehicleChangesMap")
    private IMap<String, Set<String>> vmChanges;

    public void addSubscription(Subscription setup) {

        repository.save(setup);
        logger.trace("Added subscription {}", setup);
        if (setup.isActive()) {
            activatePendingSubscription(setup.getSubscriptionId());
        }
    }

    public boolean removeSubscription(String subscriptionId) {
        return removeSubscription(subscriptionId, false);
    }

    public boolean removeSubscription(String subscriptionId, boolean force) {
        Subscription setup = repository.findBySubscriptionId(subscriptionId);

        boolean found = (setup != null);

        if (force) {
            logger.info("Completely deleting subscription-data by request.");
            activatedTimestamp.remove(subscriptionId);
            lastActivity.remove(subscriptionId);
            hitcount.remove(subscriptionId);
            objectCounter.remove(subscriptionId);
        }
        if (found) {
            setup.setActive(false);
            repository.save(setup);
        }

        logger.info("Removed subscription {}, found: {}", (setup !=null ? setup.toString():subscriptionId), found);
        return found;
    }

    public boolean touchSubscription(String subscriptionId) {
        Subscription setup = repository.findBySubscriptionId(subscriptionId);
        hit(subscriptionId);

        boolean success = (setup != null);

        logger.info("Touched subscription {}, success:{}", setup, success);
        if (success) {
            lastActivity.put(subscriptionId, Instant.now());
        }

        return success;
    }

    /**
     * Touches subscription if reported serviceStartedTime is BEFORE last activity.
     * If not, subscription is removed to trigger reestablishing subscription
     * @param subscriptionId
     * @param serviceStartedTime
     * @return
     */
    public boolean touchSubscription(String subscriptionId, ZonedDateTime serviceStartedTime) {
        Subscription setup = repository.findBySubscriptionId(subscriptionId);
        if (setup != null && serviceStartedTime != null) {
            Instant lastActivity = this.lastActivity.get(subscriptionId);
            if (lastActivity == null || serviceStartedTime.toInstant().isBefore(lastActivity)) {
                logger.info("Remote Service startTime ({}) is before lastActivity ({}) for subscription [{}]",serviceStartedTime, lastActivity, setup);
                return touchSubscription(subscriptionId);
            } else {
                logger.info("Remote service has been restarted, reestablishing subscription [{}]", setup);
                //Setting 'last activity' to longer ago than healthcheck accepts
                this.lastActivity.put(subscriptionId, Instant.now().minusSeconds((HEALTHCHECK_INTERVAL_FACTOR+1) * setup.getHeartbeatInterval().getSeconds()));
            }
        }
        return false;
    }

    public Subscription get(String subscriptionId) {

        return repository.findBySubscriptionId(subscriptionId);
    }

    private void hit(String subscriptionId) {
        int counter = (hitcount.get(subscriptionId) != null ? hitcount.get(subscriptionId):0);
        hitcount.put(subscriptionId, counter+1);
    }

    public void incrementObjectCounter(Subscription subscription, int size) {

        String subscriptionId = subscription.getSubscriptionId();
        if (subscriptionId != null) {
            BigInteger counter = (objectCounter.get(subscriptionId) != null ? objectCounter.get(subscriptionId) : new BigInteger("0"));
            objectCounter.put(subscriptionId, counter.add(BigInteger.valueOf(size)));
        }
    }

    public boolean isActiveSubscription(String subscriptionId) {
        Subscription subscription = repository.findBySubscriptionId(subscriptionId);
        if (subscription != null) {
            return subscription.isActive();
        }
        return false;
    }

    public boolean activatePendingSubscription(String subscriptionId) {
        Subscription subscription = repository.findBySubscriptionId(subscriptionId);

        if (subscription != null) {
            subscription.setActive(true);

            repository.save(subscription);

            lastActivity.put(subscriptionId, Instant.now());
            activatedTimestamp.put(subscriptionId, Instant.now());
            logger.info("Pending subscription {} activated", repository.findBySubscriptionId(subscriptionId));

            if (!dataReceived.containsKey(subscriptionId)) {
                dataReceived(subscriptionId);
            }

            return true;
        }

        logger.warn("Pending subscriptionId [{}] NOT found", subscriptionId);
        return false;
    }

    public Boolean isSubscriptionHealthy(String subscriptionId) {
        return isSubscriptionHealthy(subscriptionId, HEALTHCHECK_INTERVAL_FACTOR);
    }
    public Boolean isSubscriptionHealthy(String subscriptionId, int healthCheckIntervalFactor) {
        Instant instant = lastActivity.get(subscriptionId);

        if (instant == null) {
            //Subscription has not had any activity, and may not have been started yet - flag as healthy
            return true;
        }

        logger.trace("SubscriptionId [{}], last activity {}.", subscriptionId, instant);

        Subscription activeSubscription = repository.findBySubscriptionId(subscriptionId);
        if (activeSubscription != null && activeSubscription.isActive()) {

            Duration heartbeatInterval = activeSubscription.getHeartbeatInterval();
            if (heartbeatInterval == null) {
                heartbeatInterval = Duration.ofMinutes(5);
            }

            long allowedInterval = heartbeatInterval.toMillis() * healthCheckIntervalFactor;

            if (instant.isBefore(Instant.now().minusMillis(allowedInterval))) {
                //Subscription exists, but there has not been any activity recently
                return false;
            }

            if (activeSubscription.getSubscriptionMode().equals(SubscriptionMode.SUBSCRIBE)) {
                //Only actual subscriptions have an expiration - NOT request/response-"subscriptions"

                //If active subscription has existed longer than "initial subscription duration" - restart
                if (activatedTimestamp.get(subscriptionId) != null && activatedTimestamp.get(subscriptionId)
                        .plusSeconds(
                                activeSubscription.getDurationOfSubscription().getSeconds()
                        ).isBefore(Instant.now())) {
                    logger.info("Subscription  [{}] has lasted longer than initial subscription duration ", activeSubscription.toString());
                    return false;
                }
            }

        }

        return true;
    }

    public boolean isSubscriptionRegistered(String subscriptionId) {
        return repository.findBySubscriptionId(subscriptionId) != null;
    }

    public JSONObject buildStats() {
        JSONObject result = new JSONObject();
        JSONArray stats = new JSONArray();
        List<Subscription> subscriptions = repository.findAll();
        stats.addAll(subscriptions.stream()
                .map(subscription -> getJsonObject(subscription))
                .filter(json -> json != null)
                .collect(Collectors.toList()));

        result.put("subscriptions", stats);

        result.put("environment", environment);
        result.put("serverStarted", formatTimestamp(siriObjectFactory.serverStartTime));
        result.put("secondsSinceDataReceived", healthManager.getSecondsSinceDataReceived());
        JSONObject count = new JSONObject();
        count.put("sx", sx.getSize());
        count.put("et", et.getSize());
        count.put("vm", vm.getSize());
        count.put("pt", pt.getSize());
        count.put("sxChanges", sxChanges.size());
        count.put("etChanges", etChanges.size());
        count.put("vmChanges", vmChanges.size());
        count.put("ptChanges", ptChanges.size());

        result.put("elements", count);

        return result;
    }

    private JSONObject getJsonObject(Subscription setup) {
        if (setup == null) {
            return null;
        }
        JSONObject obj = setup.toJSON();
        obj.put("activated",formatTimestamp(activatedTimestamp.get(setup.getSubscriptionId())));
        obj.put("lastActivity",""+formatTimestamp(lastActivity.get(setup.getSubscriptionId())));
        obj.put("lastDataReceived",""+formatTimestamp(dataReceived.get(setup.getSubscriptionId())));
        if (!setup.isActive()) {
            obj.put("status", "deactivated");
            obj.put("healthy",null);
            obj.put("flagAsNotReceivingData", false);
        } else {
            obj.put("status", "active");
            obj.put("healthy", isSubscriptionHealthy(setup.getSubscriptionId()));
            obj.put("flagAsNotReceivingData", (dataReceived.get(setup.getSubscriptionId()) != null && (dataReceived.get(setup.getSubscriptionId())).isBefore(Instant.now().minusSeconds(1800))));
        }
        obj.put("hitcount",hitcount.get(setup.getSubscriptionId()));
        obj.put("objectcount", objectCounter.get(setup.getSubscriptionId()));

        JSONObject urllist = new JSONObject();
        for (RequestType s : setup.getUrlMap().keySet()) {
            urllist.put(s.name(), setup.getUrlMap().get(s));
        }
        obj.put("urllist", urllist);

        return obj;
    }

    private String formatTimestamp(Instant instant) {
        if (instant != null) {
            return formatter.format(instant);
        }
        return "";
    }

    public void stopSubscription(String subscriptionId) {

        Subscription subscription = repository.findBySubscriptionId(subscriptionId);
        if (subscription != null) {
            subscription.setActive(false);
            repository.save(subscription);

            logger.info("Handled request to cancel subscription ", subscription);
        }
    }

    public void startSubscription(String subscriptionId) {
        Subscription subscription = repository.findBySubscriptionId(subscriptionId);
        if (subscription != null) {
            subscription.setActive(true);
            activatePendingSubscription(subscriptionId);
            logger.info("Handled request to start subscription ", subscription);
        }
    }

    public Set<String> getAllUnhealthySubscriptions(int allowedInactivitySeconds) {
        List<Subscription> all = repository.findAll();
        Set<String> unhealthyVendors = all
                .stream()
                .filter(subscription -> isActiveSubscription(subscription.getSubscriptionId()))
                .filter(subscription -> !isSubscriptionReceivingData(subscription.getSubscriptionId(), allowedInactivitySeconds))
                .map(subscription -> subscription.getVendor())
                .collect(Collectors.toSet());

        return unhealthyVendors;
    }

    private boolean isSubscriptionReceivingData(String subscriptionId, long allowedInactivitySeconds) {
        if (!isActiveSubscription(subscriptionId)) {
            return true;
        }
        boolean isReceiving = true;
        Instant lastDataReceived = dataReceived.get(subscriptionId);
        if (lastDataReceived != null) {
            isReceiving = (Instant.now().minusSeconds(allowedInactivitySeconds).isBefore(lastDataReceived));
        }
        return isReceiving;
    }

    public void dataReceived(String subscriptionId) {
        touchSubscription(subscriptionId);
        if (isActiveSubscription(subscriptionId)) {
            dataReceived.put(subscriptionId, Instant.now());
        }
    }
}
