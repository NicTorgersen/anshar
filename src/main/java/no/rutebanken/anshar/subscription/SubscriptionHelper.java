package no.rutebanken.anshar.subscription;

import no.rutebanken.anshar.subscription.enums.ServiceType;
import no.rutebanken.anshar.subscription.models.Subscription;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SubscriptionHelper {
    public static String buildUrl(Subscription subscription) {
        return buildUrl(subscription, true);
    }

    public static String buildUrl(Subscription subscription, boolean includeServerAddress) {
        return (includeServerAddress ? subscription.getAddress():"") + MessageFormat.format("/{0}/{1}/{2}/{3}", subscription.getVersion(), subscription.getServiceType() == ServiceType.REST ? "rs" : "ws", subscription.getVendor(), subscription.getSubscriptionId());
    }

    public static String getStartSubscriptionRouteName(Subscription subscription) {
        return getRouteName(subscription, "start");
    }
    public static String getCancelSubscriptionRouteName(Subscription subscription) {
        return getRouteName(subscription, "cancel");
    }
    public static String getCheckStatusRouteName(Subscription subscription) {
        return getRouteName(subscription, "checkstatus");
    }
    public static String getRequestResponseRouteName(Subscription subscription) {
        return getRouteName(subscription, "request_response");
    }
    public static String getServiceRequestRouteName(Subscription subscription) {
        return getRouteName(subscription, "execute_request_response");
    }

    private static String getRouteName(Subscription subscription, String prefix) {
        return prefix + subscription.getSubscriptionId();
    }

    public static Map<Class, Set<Object>> getFilterMap(Subscription subscription) {
        if (subscription.getFilterMapPreset() != null) {
            return new FilterMapPresets().get(subscription.getFilterMapPreset());
        }
        return new HashMap<>();
    }
}
