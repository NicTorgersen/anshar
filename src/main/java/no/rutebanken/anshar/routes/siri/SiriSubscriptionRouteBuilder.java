package no.rutebanken.anshar.routes.siri;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import no.rutebanken.anshar.subscription.SubscriptionManager;
import no.rutebanken.anshar.subscription.SubscriptionSetup;
import org.apache.camel.builder.RouteBuilder;
import org.rutebanken.siri20.util.SiriXml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.Siri;
import uk.org.siri.siri20.SubscriptionResponseStructure;
import uk.org.siri.siri20.TerminateSubscriptionResponseStructure;

import javax.xml.bind.JAXBException;

public abstract class SiriSubscriptionRouteBuilder extends RouteBuilder {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    NamespacePrefixMapper customNamespacePrefixMapper;
    SubscriptionSetup subscriptionSetup;

    Siri handleSiriResponse(String xml) {
        try {
            Siri siri = SiriXml.parseXml(xml);

            if (siri.getTerminateSubscriptionResponse() != null) {
                TerminateSubscriptionResponseStructure response = siri.getTerminateSubscriptionResponse();
                response.getTerminationResponseStatuses().forEach(s -> {
                    boolean removed = SubscriptionManager.removeSubscription(s.getSubscriptionRef().getValue());
                    logger.trace("Subscription " + s.getSubscriptionRef().getValue() + " terminated: " + removed);
                });
            }
            return siri;
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return null;
    }

    void handleSubscriptionResponse(SubscriptionResponseStructure response, String responseCode) {
        if (response.getResponseStatuses().isEmpty()) {
            if ("200".equals(responseCode)) {
                SubscriptionManager.addSubscription(subscriptionSetup.getSubscriptionId(), subscriptionSetup);
            }
        } else {
            response.getResponseStatuses().forEach(s -> {
                if (s.isStatus() != null && s.isStatus()) {
                    SubscriptionManager.addSubscription(s.getSubscriptionRef().getValue(), subscriptionSetup);
                } else if (s.getErrorCondition() != null) {
                    logger.error("Error starting subscription:  {}", (s.getErrorCondition().getDescription() != null ? s.getErrorCondition().getDescription().getValue():""));
                } else {
                    SubscriptionManager.addSubscription(s.getSubscriptionRef().getValue(), subscriptionSetup);
                }
            });
        }
    }

    String getTimeout() {
        long heartbeatIntervalMillis = subscriptionSetup.getHeartbeatInterval().toMillis();
        int timeout = (int) heartbeatIntervalMillis / 2;

        return "?httpClient.socketTimeout=" + timeout + "&httpClient.connectTimeout=" + timeout;
    }

    void initTriggerRoutes() {
        from("direct:" + subscriptionSetup.getStartSubscriptionRouteName())
                .routeId(subscriptionSetup.getStartSubscriptionRouteName())
                .log("Triggering start of " + subscriptionSetup)
                .choice()
                .when(p ->subscriptionSetup.isActive())
                .to("direct:delayedStart" + subscriptionSetup.getSubscriptionId())
                .otherwise()
                .log("Subscription not active - ignoring " + subscriptionSetup)
                .endChoice()
                ;

        from("direct:" + subscriptionSetup.getCancelSubscriptionRouteName())
                .routeId(subscriptionSetup.getCancelSubscriptionRouteName())
                .log("Triggering cancel of " + subscriptionSetup)
                .to("direct:delayedCancel"+subscriptionSetup.getSubscriptionId());
    }
}
