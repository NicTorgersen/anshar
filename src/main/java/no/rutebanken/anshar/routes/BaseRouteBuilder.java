package no.rutebanken.anshar.routes;

import no.rutebanken.anshar.subscription.RequestType;
import no.rutebanken.anshar.subscription.SubscriptionManager;
import no.rutebanken.anshar.subscription.enums.SubscriptionType;
import no.rutebanken.anshar.subscription.models.Subscription;
import org.apache.camel.component.hazelcast.policy.HazelcastRoutePolicy;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.RouteContext;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.spring.SpringRouteBuilder;

import java.util.List;
import java.util.Map;

import static no.rutebanken.anshar.routes.Constants.SINGLETON_ROUTE_DEFINITION_GROUP_NAME;
import static no.rutebanken.anshar.routes.siri.SiriRequestFactory.getCamelUrl;

/**
 * Defines common route behavior.
 */
public abstract class BaseRouteBuilder extends SpringRouteBuilder {

    protected SubscriptionManager subscriptionManager;

    protected CamelConfiguration config;

    protected BaseRouteBuilder(CamelConfiguration config, SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
        this.config = config;
    }

    @Override
    public void configure() throws Exception {
        errorHandler(transactionErrorHandler()
                .logExhausted(true)
                .logRetryStackTrace(true));
    }
    /**
     * Create a new singleton route definition from URI. Only one such route should be active throughout the cluster at any time.
     */
    protected RouteDefinition singletonFrom(String uri, String routeId) {
        return this.from(uri)
                .group(SINGLETON_ROUTE_DEFINITION_GROUP_NAME)
                .routeId(routeId)
                .autoStartup(true);
    }


    protected boolean requestData(String subscriptionId, String fromRouteId) {
        Subscription subscription = subscriptionManager.get(subscriptionId);

        boolean isLeader = isLeader(fromRouteId);
        log.debug("isActive: {}, isLeader {}: {}", subscription.isActive(), isLeader, subscription);

        return (isLeader & subscription.isActive());
    }

    protected boolean isLeader(String routeId) {
        RouteContext routeContext = getContext().getRoute(routeId).getRouteContext();
        List<RoutePolicy> routePolicyList = routeContext.getRoutePolicyList();
        if (routePolicyList != null) {
            for (RoutePolicy routePolicy : routePolicyList) {
                if (routePolicy instanceof HazelcastRoutePolicy) {
                    return ((HazelcastRoutePolicy) (routePolicy)).isLeader();
                }
            }
        }
        return false;
    }


    protected String getRequestUrl(Subscription subscription) throws ServiceNotSupportedException {
        Map<RequestType, String> urlMap = subscription.getUrlMap();
        String url;
        if (subscription.getSubscriptionType() == SubscriptionType.ESTIMATED_TIMETABLE) {
            url = urlMap.get(RequestType.GET_ESTIMATED_TIMETABLE);
        } else if (subscription.getSubscriptionType() == SubscriptionType.VEHICLE_MONITORING) {
            url = urlMap.get(RequestType.GET_VEHICLE_MONITORING);
        } else if (subscription.getSubscriptionType() == SubscriptionType.SITUATION_EXCHANGE) {
            url = urlMap.get(RequestType.GET_SITUATION_EXCHANGE);
        } else {
            throw new ServiceNotSupportedException();
        }
        return getCamelUrl(url);
    }

    protected String getSoapAction(Subscription subscription) throws ServiceNotSupportedException {

        if (subscription.getSubscriptionType() == SubscriptionType.ESTIMATED_TIMETABLE) {
            return "GetEstimatedTimetableRequest";
        } else if (subscription.getSubscriptionType() == SubscriptionType.VEHICLE_MONITORING) {
            return "GetVehicleMonitoring";
        } else if (subscription.getSubscriptionType() == SubscriptionType.SITUATION_EXCHANGE) {
            return "GetSituationExchange";
        } else {
            throw new ServiceNotSupportedException();
        }
    }

}
