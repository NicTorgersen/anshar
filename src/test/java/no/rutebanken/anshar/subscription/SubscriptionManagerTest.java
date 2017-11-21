package no.rutebanken.anshar.subscription;

import no.rutebanken.anshar.App;
import no.rutebanken.anshar.subscription.enums.ServiceType;
import no.rutebanken.anshar.subscription.enums.SubscriptionMode;
import no.rutebanken.anshar.subscription.enums.SubscriptionType;
import no.rutebanken.anshar.subscription.models.Subscription;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.MOCK, classes = App.class)
public class SubscriptionManagerTest {

    @Autowired
    private SubscriptionManager subscriptionManager = mock(SubscriptionManager.class);

    @Before
    public void setUp() {

    }

    @Test
    public void activeSubscriptionIsHealthy() throws InterruptedException {
        long subscriptionDurationSec = 1;
        Subscription subscriptionSoonToExpire = createSubscription(subscriptionDurationSec);
        String subscriptionId = subscriptionSoonToExpire.getSubscriptionId();
        subscriptionManager.addSubscription(subscriptionSoonToExpire);
        subscriptionManager.activatePendingSubscription(subscriptionId);
        subscriptionManager.touchSubscription(subscriptionId);

        assertTrue(subscriptionManager.isSubscriptionHealthy(subscriptionId));

        Thread.sleep(1000*subscriptionDurationSec * subscriptionManager.HEALTHCHECK_INTERVAL_FACTOR + 150);

        assertFalse(subscriptionManager.isSubscriptionHealthy(subscriptionId));
    }

    @Test
    public void activeSubscriptionNoHeartbeat() throws InterruptedException {
        long subscriptionDurationSec = 180;
        Subscription activeSubscription = createSubscription(subscriptionDurationSec, Duration.ofMillis(150));
        String subscriptionId = activeSubscription.getSubscriptionId();

        subscriptionManager.addSubscription(activeSubscription);
        subscriptionManager.activatePendingSubscription(subscriptionId);
        subscriptionManager.touchSubscription(subscriptionId);

        assertTrue(subscriptionManager.isSubscriptionHealthy(subscriptionId));

        Thread.sleep(activeSubscription.getHeartbeatInterval().toMillis()*subscriptionManager.HEALTHCHECK_INTERVAL_FACTOR+150);

        assertFalse(subscriptionManager.isSubscriptionHealthy(subscriptionId));
    }

    @Test
    public void pendingSubscriptionIsHealthy() throws InterruptedException {
        long subscriptionDurationSec = 1;
        Subscription pendingSubscription = createSubscription(subscriptionDurationSec, Duration.ofMillis(150));
        pendingSubscription.setActive(false);
        String subscriptionId = pendingSubscription.getSubscriptionId();
        subscriptionManager.addSubscription(pendingSubscription);

        subscriptionManager.activatePendingSubscription(subscriptionId);
        subscriptionManager.touchSubscription(subscriptionId);

        assertTrue(subscriptionManager.isSubscriptionHealthy(subscriptionId));

        Thread.sleep(pendingSubscription.getHeartbeatInterval().toMillis()*subscriptionManager.HEALTHCHECK_INTERVAL_FACTOR+150);

        assertFalse(subscriptionManager.isSubscriptionHealthy(subscriptionId));
    }

    @Test
    public void notStartedSubscriptionIsHealthy() throws InterruptedException {

        long subscriptionDurationSec = 1;
        Subscription pendingSubscription = createSubscription(subscriptionDurationSec, Duration.ofMillis(150));
        pendingSubscription.setActive(false);

        String subscriptionId = pendingSubscription.getSubscriptionId();

        subscriptionManager.addSubscription(pendingSubscription);

        assertTrue(subscriptionManager.isSubscriptionHealthy(subscriptionId));

        Thread.sleep(pendingSubscription.getHeartbeatInterval().toMillis()* subscriptionManager.HEALTHCHECK_INTERVAL_FACTOR + 150);

        assertTrue(subscriptionManager.isSubscriptionHealthy(subscriptionId));
    }

    @Test
    public void testCheckStatusResponseOK() throws InterruptedException {
        long subscriptionDurationSec = 180;
        Subscription subscription = createSubscription(subscriptionDurationSec);
        subscriptionManager.addSubscription(subscription);
        subscriptionManager.activatePendingSubscription(subscription.getSubscriptionId());

        ZonedDateTime serviceStartedTime = ZonedDateTime.now().minusMinutes(1);
        boolean touched = subscriptionManager.touchSubscription(subscription.getSubscriptionId(), serviceStartedTime);
        assertTrue(touched);
        assertTrue(subscriptionManager.isSubscriptionHealthy(subscription.getSubscriptionId()));

        serviceStartedTime = ZonedDateTime.now().plusMinutes(1);
        touched = subscriptionManager.touchSubscription(subscription.getSubscriptionId(), serviceStartedTime);
        assertFalse(touched);
        assertFalse(subscriptionManager.isSubscriptionHealthy(subscription.getSubscriptionId()));
    }

    @Test
    public void testAddSubscription() {
        Subscription subscription = createSubscription(1);
        assertFalse("Subscription already marked as registered", subscriptionManager.isSubscriptionRegistered(subscription.getSubscriptionId()));
        subscriptionManager.addSubscription(subscription);
        subscriptionManager.activatePendingSubscription(subscription.getSubscriptionId());

        assertTrue("Subscription not marked as registered", subscriptionManager.isSubscriptionRegistered(subscription.getSubscriptionId()));
        assertTrue("Subscription not marked as active", subscriptionManager.isActiveSubscription(subscription.getSubscriptionId()));


        assertNotNull("Subscription not found", subscriptionManager.get(subscription.getSubscriptionId()));
    }

    @Test
    public void testAddAndActivatePendingSubscription() {
        Subscription subscription = createSubscription(1);
        assertFalse("Unknown subscription has been found",subscriptionManager.isSubscriptionRegistered(subscription.getSubscriptionId()));
        subscription.setActive(false);

        subscriptionManager.addSubscription(subscription);

        assertNotNull("Pending subscription not found", subscriptionManager.get(subscription.getSubscriptionId()));

        assertTrue("Subscription not marked as registered", subscriptionManager.isSubscriptionRegistered(subscription.getSubscriptionId()));
        assertFalse("Subscription marked as active", subscriptionManager.isActiveSubscription(subscription.getSubscriptionId()));
        assertTrue("Subscription not healthy", subscriptionManager.isSubscriptionHealthy(subscription.getSubscriptionId()));

        assertTrue("Activating pending subscription not returning successfully", subscriptionManager.activatePendingSubscription(subscription.getSubscriptionId()));

        //Activating already activated subscription should be ignored
        assertTrue("Activating already activated subscription not returning successfully", subscriptionManager.activatePendingSubscription(subscription.getSubscriptionId()));

        assertTrue("Subscription not marked as registered", subscriptionManager.isSubscriptionRegistered(subscription.getSubscriptionId()));
        assertTrue("Subscription not marked as active", subscriptionManager.isActiveSubscription(subscription.getSubscriptionId()));
    }

    @Test
    public void testAddAndTouchPendingSubscription() {
        Subscription subscription = createSubscription(1);
        subscription.setActive(false);
        assertFalse(subscriptionManager.isSubscriptionRegistered(subscription.getSubscriptionId()));

        subscriptionManager.addSubscription(subscription);

        assertTrue("Subscription not marked as registered", subscriptionManager.isSubscriptionRegistered(subscription.getSubscriptionId()));
        assertFalse("Subscription marked as active", subscriptionManager.isActiveSubscription(subscription.getSubscriptionId()));

        assertTrue("Subscription not healthy", subscriptionManager.isSubscriptionHealthy(subscription.getSubscriptionId()));

        subscriptionManager.touchSubscription(subscription.getSubscriptionId());

        assertTrue("Subscription not marked as registered", subscriptionManager.isSubscriptionRegistered(subscription.getSubscriptionId()));
        assertFalse("Subscription marked as active", subscriptionManager.isActiveSubscription(subscription.getSubscriptionId()));
    }

    @Test
    public void testRemoveSubscription() {
        Subscription subscription = createSubscription(1);
        assertFalse(subscriptionManager.isSubscriptionRegistered(subscription.getSubscriptionId()));

        subscriptionManager.addSubscription(subscription);
        subscriptionManager.activatePendingSubscription(subscription.getSubscriptionId());

        assertTrue("Subscription not registered", subscriptionManager.isSubscriptionRegistered(subscription.getSubscriptionId()));
        assertTrue("Subscription not marked as active", subscriptionManager.isActiveSubscription(subscription.getSubscriptionId()));

        subscriptionManager.removeSubscription(subscription.getSubscriptionId());
        assertFalse("Removed subscription marked as active", subscriptionManager.isActiveSubscription(subscription.getSubscriptionId()));
    }

    @Test
    public void testForceRemoveSubscription() {
        Subscription subscription = createSubscription(1);
        assertFalse(subscriptionManager.isSubscriptionRegistered(subscription.getSubscriptionId()));

        subscriptionManager.addSubscription(subscription);
        subscriptionManager.activatePendingSubscription(subscription.getSubscriptionId());

        subscriptionManager.removeSubscription(subscription.getSubscriptionId(), true);
        assertFalse("Removed subscription marked as active", subscriptionManager.isActiveSubscription(subscription.getSubscriptionId()));
    }

    @Test
    public void testStatsObjectCounterHugeNumber() {
        Subscription subscription = createSubscription(1);
        assertFalse(subscriptionManager.isSubscriptionRegistered(subscription.getSubscriptionId()));

        subscriptionManager.addSubscription(subscription);
        subscriptionManager.activatePendingSubscription(subscription.getSubscriptionId());

        for (int i = 0; i < 10; i++) {
            subscriptionManager.incrementObjectCounter(subscription, Integer.MAX_VALUE);
        }

        JSONObject jsonObject = subscriptionManager.buildStats();
        assertNotNull(jsonObject.get("subscriptions"));
        assertTrue(jsonObject.get("subscriptions") instanceof JSONArray);

        JSONArray subscriptions = (JSONArray) jsonObject.get("subscriptions");
        assertTrue(subscriptions.size() > 0);

        boolean verifiedCounter = false;
        for (Object object : subscriptions) {
            JSONObject jsonStats = (JSONObject) object;
            if (subscription.getSubscriptionId().equals(jsonStats.get("subscriptionId"))) {
                assertNotNull(jsonStats.get("objectcount"));
                assertTrue(jsonStats.get("objectcount").toString().length() > String.valueOf(Integer.MAX_VALUE).length());
                verifiedCounter = true;
            }
        }
        assertTrue("Counter has not been verified", verifiedCounter);
    }

    @Test
    public void testStatByteCounter() {
        Subscription subscription = createSubscription(1);
        assertFalse(subscriptionManager.isSubscriptionRegistered(subscription.getSubscriptionId()));

        subscriptionManager.addSubscription(subscription);
        subscriptionManager.activatePendingSubscription(subscription.getSubscriptionId());

        int sum = 0;
        int increment = 999;
        for (int i = 1; i < 10;i++) {
            sum += increment;
            subscriptionManager.incrementObjectCounter(subscription, increment);
        }

        JSONObject jsonObject = subscriptionManager.buildStats();
        assertNotNull(jsonObject.get("subscriptions"));
        assertTrue(jsonObject.get("subscriptions") instanceof JSONArray);

        JSONArray subscriptions = (JSONArray) jsonObject.get("subscriptions");
        assertTrue(subscriptions.size() > 0);

        boolean verifiedCounter = false;
        for (Object object : subscriptions) {
            JSONObject jsonStats = (JSONObject) object;
            if (subscription.getSubscriptionId().equals(jsonStats.get("subscriptionId"))) {
                assertEquals("" + sum, "" + jsonStats.get("objectcount"));
                verifiedCounter = true;
            }
        }
        assertTrue("Counter has not been verified", verifiedCounter);
    }

    @Test
    public void testIsSubscriptionRegistered() {

        assertFalse("Unknown subscription has been activated", subscriptionManager.activatePendingSubscription("RandomSubscriptionId"));
        assertFalse("Unknown subscription reported as registered", subscriptionManager.isSubscriptionRegistered("RandomSubscriptionId"));
    }

    @Test
    public void testAddSubscriptionAndReceivingData() {
        Subscription subscription = createSubscription(1000);
        subscription.setVendor("VIPVendor");
        subscription.setActive(true);

        String subscriptionId = subscription.getSubscriptionId();
        subscriptionManager.addSubscription(subscription);
        assertTrue(subscriptionManager.isSubscriptionHealthy(subscriptionId));
        subscriptionManager.dataReceived(subscriptionId);

        Set<String> allUnhealthySubscriptions = subscriptionManager.getAllUnhealthySubscriptions(1);
        assertFalse(allUnhealthySubscriptions.contains(subscription.getVendor()));
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Set<String> allUnhealthySubscriptions_2 = subscriptionManager.getAllUnhealthySubscriptions(0);
        assertTrue(allUnhealthySubscriptions_2.contains(subscription.getVendor()));
    }

    private Subscription createSubscription(long initialDuration) {
        return createSubscription(initialDuration, Duration.ofMinutes(4));
    }

    private Subscription createSubscription(long initialDuration, Duration heartbeatInterval) {
        Subscription sub = new Subscription();
        sub.setSubscriptionType(SubscriptionType.SITUATION_EXCHANGE);
        sub.setSubscriptionMode(SubscriptionMode.SUBSCRIBE);
        sub.setAddress("http://localhost");
        sub.setHeartbeatInterval(heartbeatInterval);
        sub.setUpdateInterval(Duration.ofSeconds(1));
        sub.setOperatorNamespace("http://www.kolumbus.no/siri");
        sub.setUrlMap(new HashMap<>());
        sub.setVersion("1.4");
        sub.setVendor("SwarcoMizar");
        sub.setDatasetId("tst");
        sub.setServiceType(ServiceType.SOAP);
        sub.setMappingAdapters(new ArrayList<>());
        sub.setFilterMapPreset(null);
        sub.setIdMappingPrefixes(new ArrayList<String>());
        sub.setSubscriptionId(UUID.randomUUID().toString());
        sub.setRequestorRef("RutebankenDEV");
        sub.setDurationOfSubscription(Duration.ofSeconds(initialDuration));
        sub.setActive(true);

        return sub;
    }
}
