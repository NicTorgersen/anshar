package no.rutebanken.anshar.subscription;

import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import no.rutebanken.anshar.subscription.enums.ServiceType;
import no.rutebanken.anshar.subscription.enums.SubscriptionMode;
import no.rutebanken.anshar.subscription.enums.SubscriptionType;
import no.rutebanken.anshar.subscription.models.Subscription;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static junit.framework.TestCase.*;

public class SubscriptionSetupTest {

    private Subscription setup_1;
    private Subscription setup_2;

    @Before
    public void setUp() {
        HashMap<RequestType, String> urlMap_1 = new HashMap<>();
        urlMap_1.put(RequestType.SUBSCRIBE, "http://localhost:1234/subscribe");

        HashMap<RequestType, String> urlMap_2 = new HashMap<>();
        urlMap_2.putAll(urlMap_1);

        setup_1 = new Subscription();
        setup_1.setSubscriptionType(SubscriptionType.SITUATION_EXCHANGE);
        setup_1.setSubscriptionMode(SubscriptionMode.SUBSCRIBE);
        setup_1.setAddress("http://localhost");
        setup_1.setHeartbeatInterval(Duration.ofHours(1));
        setup_1.setUpdateInterval(Duration.ofHours(1));
        setup_1.setOperatorNamespace("http://www.kolumbus.no/siri");
        setup_1.setUrlMap(urlMap_1);
        setup_1.setVersion("1.4");
        setup_1.setVendor("SwarcoMizar");
        setup_1.setDatasetId("tst");
        setup_1.setServiceType(ServiceType.SOAP);
        setup_1.setMappingAdapters(new ArrayList<ValueAdapter>());
        setup_1.setFilterMapPreset(null);
        setup_1.setIdMappingPrefixes(new ArrayList<String>());
        setup_1.setSubscriptionId(UUID.randomUUID().toString());
        setup_1.setRequestorRef("RutebankenDEV");
        setup_1.setDurationOfSubscription(Duration.ofSeconds((long) 1000));
        setup_1.setActive(true);

        setup_2 = new Subscription();
        setup_2.setSubscriptionType(SubscriptionType.SITUATION_EXCHANGE);
        setup_2.setSubscriptionMode(SubscriptionMode.SUBSCRIBE);
        setup_2.setAddress("http://localhost");
        setup_2.setHeartbeatInterval(Duration.ofHours(1));
        setup_2.setUpdateInterval(Duration.ofHours(1));
        setup_2.setOperatorNamespace("http://www.kolumbus.no/siri");
        setup_2.setUrlMap(urlMap_2);
        setup_2.setVersion("1.4");
        setup_2.setVendor("SwarcoMizar");
        setup_2.setDatasetId("tst");
        setup_2.setServiceType(ServiceType.SOAP);
        setup_2.setMappingAdapters(new ArrayList<ValueAdapter>());
        setup_2.setFilterMapPreset(null);
        setup_2.setIdMappingPrefixes(new ArrayList<String>());
        setup_2.setSubscriptionId(UUID.randomUUID().toString());
        setup_2.setRequestorRef("RutebankenDEV");
        setup_2.setDurationOfSubscription(Duration.ofSeconds((long) 1000));
        setup_2.setActive(true);
    }

    @Test
    public void testSimpleEquals() {
        assertEquals(setup_1, setup_2);
    }

    @Test
    public void testEqualsUpdatedSubscriptionType() {
        assertEquals(setup_1, setup_2);
        setup_2.setSubscriptionType(SubscriptionType.VEHICLE_MONITORING);
        assertFalse(setup_1.equals(setup_2));
    }

    @Test
    public void testEqualsUpdatedAddress() {
        assertEquals(setup_1, setup_2);
        setup_2.setAddress("http://other.address");
        assertFalse(setup_1.equals(setup_2));
    }

    @Test
    public void testEqualsUpdatedNameSpace() {
        assertEquals(setup_1, setup_2);
        setup_2.setOperatorNamespace("http://other.operator.namespace");
        assertFalse(setup_1.equals(setup_2));
    }

    @Test
    public void testEqualsUpdatedInitialDuration() {
        assertEquals(setup_1, setup_2);
        setup_2.setDurationOfSubscription(Duration.ofSeconds(setup_1.getDurationOfSubscription().getSeconds()*2));
        assertFalse(setup_1.equals(setup_2));
    }

    @Test
    public void testEqualsUpdatedUrl() {
        assertEquals(setup_1, setup_2);
        Map<RequestType, String> urlMap = setup_2.getUrlMap();
        assertTrue("urlMap does not contain expected URL", urlMap.containsKey(RequestType.SUBSCRIBE));
        urlMap.put(RequestType.SUBSCRIBE, urlMap.get(RequestType.SUBSCRIBE) + "/updated");
        assertFalse(setup_1.equals(setup_2));
    }

    @Test
    public void testEqualsAddedUrl() {
        assertEquals(setup_1, setup_2);
        Map<RequestType, String> urlMap = setup_2.getUrlMap();
        urlMap.put(RequestType.GET_VEHICLE_MONITORING, urlMap.get(RequestType.SUBSCRIBE) + "/vm");
        assertFalse(setup_1.equals(setup_2));
    }

    @Test
    public void testEqualsAlteredSubscriptionIdIgnored() {
        assertFalse(setup_1.getSubscriptionId().equals(setup_2.getSubscriptionId()));
        assertEquals(setup_1, setup_2);
    }

}
