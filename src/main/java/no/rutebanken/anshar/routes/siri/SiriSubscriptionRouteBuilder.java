package no.rutebanken.anshar.routes.siri;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import no.rutebanken.anshar.routes.BaseRouteBuilder;
import no.rutebanken.anshar.routes.CamelConfiguration;
import no.rutebanken.anshar.subscription.RequestType;
import no.rutebanken.anshar.subscription.SubscriptionManager;
import no.rutebanken.anshar.subscription.models.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

import static no.rutebanken.anshar.subscription.SubscriptionHelper.*;

@Component
public abstract class SiriSubscriptionRouteBuilder extends BaseRouteBuilder {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    
    NamespacePrefixMapper customNamespacePrefixMapper;

    Subscription subscription;

    protected boolean hasBeenStarted;

    private Instant lastCheckStatus = Instant.now();

    public SiriSubscriptionRouteBuilder(CamelConfiguration config, SubscriptionManager subscriptionManager) {
        super(config, subscriptionManager);
    }

    String getTimeout() {
        int timeout;
        Duration heartbeatInterval = subscription.getHeartbeatInterval();
        if (heartbeatInterval != null) {
            long heartbeatIntervalMillis = heartbeatInterval.toMillis();
            timeout = (int) heartbeatIntervalMillis / 2;
        } else {
            timeout = 30000;
        }

        return "?httpClient.socketTimeout=" + timeout + "&httpClient.connectTimeout=" + timeout;
    }

    String getTimeToLive() {
        return config.getTimeToLive();
    }

    void initTriggerRoutes() {
//        if (!subscriptionManager.isNewSubscription(subscription.getSubscriptionId())) {
//            logger.info("Subscription is NOT new - flagging as already started if active {}", subscription);
//            hasBeenStarted = subscriptionManager.isActiveSubscription(subscription.getSubscriptionId());
//        }
        // Assuming ALL subscriptions are hunky-dory on start-up
        if (subscriptionManager.get(subscription.getSubscriptionId()) != null) {
            // Subscription is already initialized on another pod - keep existing status
            hasBeenStarted = subscriptionManager.isActiveSubscription(subscription.getSubscriptionId());
        } else {
            // Unknown subscription or first pod to start
            hasBeenStarted = subscription.isActive();
        }

        singletonFrom("quartz2://anshar/monitor_" + subscription.getSubscriptionId() + "?fireNow=true&trigger.repeatInterval=" + 15000,
                "monitor.subscription." + subscription.getVendor())
                .choice()
                .when(p -> shouldBeStarted(p.getFromRouteId()))
                    .log("Triggering start subscription: " + subscription)
                    .process(p -> hasBeenStarted = true)
                    .to("direct:" + getStartSubscriptionRouteName(subscription)) // Start subscription
                .when(p -> shouldBeCancelled(p.getFromRouteId()))
                    .log("Triggering cancel subscription: " + subscription)
                    .process(p -> hasBeenStarted = false)
                    .to("direct:" + getCancelSubscriptionRouteName(subscription))// Cancel subscription
                .when(p -> shouldCheckStatus(p.getFromRouteId()))
                    .log("Check status: " + subscription)
                    .process(p -> lastCheckStatus = Instant.now())
                    .to("direct:" + getCheckStatusRouteName(subscription)) // Check status
                .end()
        ;

    }

    private boolean shouldCheckStatus(String routeId) {
        if (!isLeader(routeId)) {
            return false;
        }
        boolean isActive = subscriptionManager.isActiveSubscription(subscription.getSubscriptionId());
        boolean requiresCheckStatusRequest = subscription.getUrlMap().get(RequestType.CHECK_STATUS) != null;
        boolean isTimeToCheckStatus = lastCheckStatus.isBefore(Instant.now().minus(subscription.getHeartbeatInterval()));

        return isActive & requiresCheckStatusRequest & isTimeToCheckStatus;
    }

    private boolean shouldBeStarted(String routeId) {
        if (!isLeader(routeId)) {
            return false;
        }
        boolean isActive = subscriptionManager.isActiveSubscription(subscription.getSubscriptionId());

        boolean shouldBeStarted = (isActive & !hasBeenStarted);
        return shouldBeStarted;
    }

    private boolean shouldBeCancelled(String routeId) {
        if (!isLeader(routeId)) {
            return false;
        }
        boolean isActive = subscriptionManager.isActiveSubscription(subscription.getSubscriptionId());
        boolean isHealthy = subscriptionManager.isSubscriptionHealthy(subscription.getSubscriptionId());

        boolean shouldBeCancelled = (hasBeenStarted & !isActive) | (hasBeenStarted & isActive & !isHealthy);

        return shouldBeCancelled;
    }
}
