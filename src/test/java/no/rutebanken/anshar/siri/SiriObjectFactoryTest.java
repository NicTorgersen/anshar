package no.rutebanken.anshar.siri;

import no.rutebanken.anshar.routes.siri.SiriObjectFactory;
import no.rutebanken.anshar.subscription.enums.ServiceType;
import no.rutebanken.anshar.subscription.enums.SubscriptionMode;
import no.rutebanken.anshar.subscription.enums.SubscriptionType;
import no.rutebanken.anshar.subscription.models.Subscription;
import org.junit.Test;
import uk.org.siri.siri20.*;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class SiriObjectFactoryTest {

    private int hoursUntilInitialTermination = 1;

    @Test
    public void testCreateVMSubscription(){

        Subscription subscription = createSubscriptionSetup(SubscriptionType.VEHICLE_MONITORING,
                SubscriptionMode.REQUEST_RESPONSE,
                UUID.randomUUID().toString());

        Siri vmSubscriptionRequest = SiriObjectFactory.createSubscriptionRequest(subscription);
        assertNotNull(vmSubscriptionRequest.getSubscriptionRequest());
        List<VehicleMonitoringSubscriptionStructure> subscriptionRequests = vmSubscriptionRequest.getSubscriptionRequest().getVehicleMonitoringSubscriptionRequests();
        assertNotNull(subscriptionRequests);

        assertTrue(subscriptionRequests.size() == 1);

        VehicleMonitoringSubscriptionStructure vmSubscription = subscriptionRequests.get(0);
        assertNotNull(vmSubscription.getSubscriptionIdentifier());
        assertNotNull(vmSubscription.getSubscriptionIdentifier().getValue());
        assertEquals(subscription.getSubscriptionId(), vmSubscription.getSubscriptionIdentifier().getValue());

        ZonedDateTime initialTerminationTime = vmSubscription.getInitialTerminationTime();

        assertTrue("Initial terminationtime has not been calculated correctly", ZonedDateTime.now().plusHours(hoursUntilInitialTermination).minusMinutes(1).isBefore(initialTerminationTime));
        assertTrue("Initial terminationtime has not been calculated correctly", ZonedDateTime.now().plusHours(hoursUntilInitialTermination).plusMinutes(1).isAfter(initialTerminationTime));

    }

    @Test
    public void testCreateVMServiceRequest(){

        Subscription subscription = createSubscriptionSetup(SubscriptionType.VEHICLE_MONITORING,
                SubscriptionMode.REQUEST_RESPONSE,
                UUID.randomUUID().toString());

        Siri vmRequest = SiriObjectFactory.createServiceRequest(subscription);
        assertNull(vmRequest.getSubscriptionRequest());

        assertNotNull(vmRequest.getServiceRequest());

        List<VehicleMonitoringRequestStructure> vmRequests = vmRequest.getServiceRequest().getVehicleMonitoringRequests();
        assertNotNull(vmRequests);

        assertTrue(vmRequests.size() == 1);

        VehicleMonitoringRequestStructure request = vmRequests.get(0);
        assertNotNull(request);
    }

    @Test
    public void testCreateSXSubscription(){

        Subscription subscription = createSubscriptionSetup(SubscriptionType.SITUATION_EXCHANGE,
                SubscriptionMode.REQUEST_RESPONSE,
                UUID.randomUUID().toString());

        Siri sxSubscriptionRequest = SiriObjectFactory.createSubscriptionRequest(subscription);
        assertNotNull(sxSubscriptionRequest.getSubscriptionRequest());

        List<SituationExchangeSubscriptionStructure> subscriptionRequests = sxSubscriptionRequest.getSubscriptionRequest().getSituationExchangeSubscriptionRequests();
        assertNotNull(subscriptionRequests);

        assertTrue(subscriptionRequests.size() == 1);

        SituationExchangeSubscriptionStructure sxSubscription = subscriptionRequests.get(0);
        assertNotNull(sxSubscription.getSubscriptionIdentifier());
        assertNotNull(sxSubscription.getSubscriptionIdentifier().getValue());
        assertEquals(subscription.getSubscriptionId(), sxSubscription.getSubscriptionIdentifier().getValue());

        ZonedDateTime initialTerminationTime = sxSubscription.getInitialTerminationTime();

        assertTrue("Initial terminationtime has not been calculated correctly", ZonedDateTime.now().plusHours(hoursUntilInitialTermination).minusMinutes(1).isBefore(initialTerminationTime));
        assertTrue("Initial terminationtime has not been calculated correctly", ZonedDateTime.now().plusHours(hoursUntilInitialTermination).plusMinutes(1).isAfter(initialTerminationTime));



    }

    @Test
    public void testCreateSubscriptionCustomAddressfield(){

        Subscription sxSubscriptionSetup = createSubscriptionSetup(SubscriptionType.SITUATION_EXCHANGE,
                SubscriptionMode.SUBSCRIBE,
                UUID.randomUUID().toString());

        Subscription etSubscriptionSetup = createSubscriptionSetup(SubscriptionType.ESTIMATED_TIMETABLE,
                SubscriptionMode.SUBSCRIBE,
                UUID.randomUUID().toString());

        Subscription vmSubscriptionSetup = createSubscriptionSetup(SubscriptionType.VEHICLE_MONITORING,
                SubscriptionMode.SUBSCRIBE,
                UUID.randomUUID().toString());

        Subscription ptSubscriptionSetup = createSubscriptionSetup(SubscriptionType.PRODUCTION_TIMETABLE,
                SubscriptionMode.SUBSCRIBE,
                UUID.randomUUID().toString());

        Siri sxSubscriptionRequest = SiriObjectFactory.createSubscriptionRequest(sxSubscriptionSetup);
        Siri etSubscriptionRequest = SiriObjectFactory.createSubscriptionRequest(etSubscriptionSetup);
        Siri vmSubscriptionRequest = SiriObjectFactory.createSubscriptionRequest(vmSubscriptionSetup);
        Siri ptSubscriptionRequest = SiriObjectFactory.createSubscriptionRequest(ptSubscriptionSetup);

        assertNotNull(sxSubscriptionRequest.getSubscriptionRequest().getConsumerAddress());
        assertNotNull(etSubscriptionRequest.getSubscriptionRequest().getConsumerAddress());
        assertNotNull(vmSubscriptionRequest.getSubscriptionRequest().getConsumerAddress());
        assertNotNull(ptSubscriptionRequest.getSubscriptionRequest().getConsumerAddress());

        assertNull(sxSubscriptionRequest.getSubscriptionRequest().getAddress());
        assertNull(etSubscriptionRequest.getSubscriptionRequest().getAddress());
        assertNull(vmSubscriptionRequest.getSubscriptionRequest().getAddress());
        assertNull(ptSubscriptionRequest.getSubscriptionRequest().getAddress());


        ptSubscriptionSetup.setAddressFieldName("Address");
        vmSubscriptionSetup.setAddressFieldName("Address");
        etSubscriptionSetup.setAddressFieldName("Address");
        sxSubscriptionSetup.setAddressFieldName("Address");

        sxSubscriptionRequest = SiriObjectFactory.createSubscriptionRequest(sxSubscriptionSetup);
        etSubscriptionRequest = SiriObjectFactory.createSubscriptionRequest(etSubscriptionSetup);
        vmSubscriptionRequest = SiriObjectFactory.createSubscriptionRequest(vmSubscriptionSetup);
        ptSubscriptionRequest = SiriObjectFactory.createSubscriptionRequest(ptSubscriptionSetup);

        assertNotNull(sxSubscriptionRequest.getSubscriptionRequest().getAddress());
        assertNotNull(etSubscriptionRequest.getSubscriptionRequest().getAddress());
        assertNotNull(vmSubscriptionRequest.getSubscriptionRequest().getAddress());
        assertNotNull(ptSubscriptionRequest.getSubscriptionRequest().getAddress());

        assertNull(sxSubscriptionRequest.getSubscriptionRequest().getConsumerAddress());
        assertNull(etSubscriptionRequest.getSubscriptionRequest().getConsumerAddress());
        assertNull(vmSubscriptionRequest.getSubscriptionRequest().getConsumerAddress());
        assertNull(ptSubscriptionRequest.getSubscriptionRequest().getConsumerAddress());
    }

    @Test
    public void testCreateSXServiceRequest(){

        Subscription subscription = createSubscriptionSetup(SubscriptionType.SITUATION_EXCHANGE,
                SubscriptionMode.REQUEST_RESPONSE,
                UUID.randomUUID().toString());

        Siri sxRequest = SiriObjectFactory.createServiceRequest(subscription);
        assertNull(sxRequest.getSubscriptionRequest());

        assertNotNull(sxRequest.getServiceRequest());

        List<SituationExchangeRequestStructure> sxRequests = sxRequest.getServiceRequest().getSituationExchangeRequests();
        assertNotNull(sxRequests);

        assertTrue(sxRequests.size() == 1);

        SituationExchangeRequestStructure request = sxRequests.get(0);
        assertNotNull(request);
    }

    @Test
    public void testCreateETSubscription(){

        Subscription subscription = createSubscriptionSetup(SubscriptionType.ESTIMATED_TIMETABLE,
                SubscriptionMode.REQUEST_RESPONSE,
                UUID.randomUUID().toString());

        Siri vmSubscriptionRequest = SiriObjectFactory.createSubscriptionRequest(subscription);
        assertNotNull(vmSubscriptionRequest.getSubscriptionRequest());

        List<EstimatedTimetableSubscriptionStructure> subscriptionRequests = vmSubscriptionRequest.getSubscriptionRequest().getEstimatedTimetableSubscriptionRequests();
        assertNotNull(subscriptionRequests);

        assertTrue(subscriptionRequests.size() == 1);

        EstimatedTimetableSubscriptionStructure etSubscription = subscriptionRequests.get(0);
        assertNotNull(etSubscription.getSubscriptionIdentifier());
        assertNotNull(etSubscription.getSubscriptionIdentifier().getValue());
        assertEquals(subscription.getSubscriptionId(), etSubscription.getSubscriptionIdentifier().getValue());

        ZonedDateTime initialTerminationTime = etSubscription.getInitialTerminationTime();

        assertTrue("Initial terminationtime has not been calculated correctly", ZonedDateTime.now().plusHours(hoursUntilInitialTermination).minusMinutes(1).isBefore(initialTerminationTime));
        assertTrue("Initial terminationtime has not been calculated correctly", ZonedDateTime.now().plusHours(hoursUntilInitialTermination).plusMinutes(1).isAfter(initialTerminationTime));


    }

    @Test
    public void testCreateETServiceRequest(){

        Subscription subscription = createSubscriptionSetup(SubscriptionType.ESTIMATED_TIMETABLE,
                SubscriptionMode.REQUEST_RESPONSE,
                UUID.randomUUID().toString());

        Siri etRequest = SiriObjectFactory.createServiceRequest(subscription);
        assertNull(etRequest.getSubscriptionRequest());

        assertNotNull(etRequest.getServiceRequest());

        List<EstimatedTimetableRequestStructure> etRequests = etRequest.getServiceRequest().getEstimatedTimetableRequests();
        assertNotNull(etRequests);

        assertTrue(etRequests.size() == 1);

        EstimatedTimetableRequestStructure request = etRequests.get(0);
        assertNotNull(request);
    }

    @Test
    public void testCreatePTSubscription(){

        Subscription subscription = createSubscriptionSetup(SubscriptionType.PRODUCTION_TIMETABLE,
                SubscriptionMode.REQUEST_RESPONSE,
                UUID.randomUUID().toString());

        Siri vmSubscriptionRequest = SiriObjectFactory.createSubscriptionRequest(subscription);
        assertNotNull(vmSubscriptionRequest.getSubscriptionRequest());

        List<ProductionTimetableSubscriptionRequest> subscriptionRequests = vmSubscriptionRequest.getSubscriptionRequest().getProductionTimetableSubscriptionRequests();
        assertNotNull(subscriptionRequests);

        assertTrue(subscriptionRequests.size() == 1);

        ProductionTimetableSubscriptionRequest ptSubscription = subscriptionRequests.get(0);
        assertNotNull(ptSubscription.getSubscriptionIdentifier());
        assertNotNull(ptSubscription.getSubscriptionIdentifier().getValue());
        assertEquals(subscription.getSubscriptionId(), ptSubscription.getSubscriptionIdentifier().getValue());

    }

    @Test
    public void testCreatePTServiceRequest(){

        Subscription subscription = createSubscriptionSetup(SubscriptionType.PRODUCTION_TIMETABLE,
                SubscriptionMode.REQUEST_RESPONSE,
                UUID.randomUUID().toString());

        Siri ptRequest = SiriObjectFactory.createServiceRequest(subscription);
        assertNull(ptRequest.getSubscriptionRequest());

        assertNotNull(ptRequest.getServiceRequest());

        List<ProductionTimetableRequestStructure> ptRequests = ptRequest.getServiceRequest().getProductionTimetableRequests();
        assertNotNull(ptRequests);

        assertTrue(ptRequests.size() == 1);

        ProductionTimetableRequestStructure request = ptRequests.get(0);
        assertNotNull(request);
    }

    @Test
    public void testCreateTerminateSubscriptionRequest(){

        Subscription subscription = createSubscriptionSetup(SubscriptionType.VEHICLE_MONITORING,
                SubscriptionMode.REQUEST_RESPONSE,
                UUID.randomUUID().toString());

        Siri request = SiriObjectFactory.createTerminateSubscriptionRequest(subscription);
        assertNotNull(request);
    }

    @Test
    public void testCreateNullTerminateSubscriptionRequest(){

        Siri request = SiriObjectFactory.createTerminateSubscriptionRequest(null);
        assertNull(request);

        Subscription subscription = createSubscriptionSetup(SubscriptionType.VEHICLE_MONITORING,
                SubscriptionMode.REQUEST_RESPONSE,
                UUID.randomUUID().toString());

        request = SiriObjectFactory.createTerminateSubscriptionRequest(subscription);
        assertNotNull(request);

    }

    private Subscription createSubscriptionSetup(SubscriptionType type, SubscriptionMode mode, String subscriptionId) {
        return createSubscriptionSetup(type, mode, subscriptionId, "RutebankenDev");
    }

    private Subscription createSubscriptionSetup(SubscriptionType type, SubscriptionMode mode, String subscriptionId, String requestorRef) {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionType(type);
        subscription.setSubscriptionMode(mode);
        subscription.setAddress("http://localhost");
        subscription.setHeartbeatInterval(Duration.of(30, ChronoUnit.SECONDS));
        subscription.setOperatorNamespace("http://www.kolumbus.no/siri");
        subscription.setUrlMap(new HashMap<>());
        subscription.setVersion("1.4");
        subscription.setVendor("dumvm");
        subscription.setDatasetId("dum");
        subscription.setServiceType(ServiceType.SOAP);
        subscription.setSubscriptionId(subscriptionId);
        subscription.setRequestorRef(requestorRef);
        subscription.setDurationOfSubscription(Duration.of(hoursUntilInitialTermination, ChronoUnit.HOURS));
        subscription.setActive(true);
        return subscription;
    }
}
