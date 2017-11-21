package no.rutebanken.anshar.subscription;

import com.google.common.base.Preconditions;
import com.hazelcast.core.IMap;
import no.rutebanken.anshar.messages.collections.HealthCheckKey;
import no.rutebanken.anshar.routes.CamelConfiguration;
import no.rutebanken.anshar.routes.siri.*;
import no.rutebanken.anshar.routes.siri.adapters.Mapping;
import no.rutebanken.anshar.routes.siri.handlers.SiriHandler;
import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import no.rutebanken.anshar.subscription.enums.ServiceType;
import no.rutebanken.anshar.subscription.enums.SubscriptionMode;
import no.rutebanken.anshar.subscription.enums.SubscriptionType;
import no.rutebanken.anshar.subscription.models.Subscription;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;

@Service
public class SubscriptionInitializer implements CamelContextAware, ApplicationContextAware {
    private Logger logger = LoggerFactory.getLogger(SubscriptionInitializer.class);

    @Autowired
    private SubscriptionManager subscriptionManager;

    @Autowired
    private SubscriptionRepository repository;

    @Autowired
    @Qualifier("getHealthCheckMap")
    private IMap<Enum<HealthCheckKey>, Instant> healthCheckMap;

    @Autowired
    private SubscriptionConfig subscriptionConfig;

    @Autowired
    SiriHandler handler;

    @Autowired
    CamelConfiguration camelConfiguration;

    private CamelContext camelContext;

    private ApplicationContext applicationContext;

    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @PostConstruct
    void createSubscriptions() {
        camelContext.setUseMDCLogging(true);


        final Map<String, Object> myFoos = applicationContext.getBeansWithAnnotation(Mapping.class);
        final Map<String, Class> mappingAdaptersById = new HashMap<>();
        for (final Object myFoo : myFoos.values()) {
            final Class<? extends Object> mappingAdapterClass = myFoo.getClass();
            final Mapping annotation = mappingAdapterClass.getAnnotation(Mapping.class);
            mappingAdaptersById.put(annotation.id(), mappingAdapterClass);
        }

        logger.info("Initializing subscriptions for environment: {}", camelConfiguration.getEnvironment());


        if (subscriptionConfig != null) {
            List<Subscription> subscriptions = repository.findAll();

            if (subscriptions == null || subscriptions.isEmpty()) {
                // Initially adding subscriptions in database
                List<YamlSubscriptionSetup> yamlSubscriptions = subscriptionConfig.getSubscriptions();
                subscriptions = new ArrayList<>();
                for (YamlSubscriptionSetup ymlSubscription : yamlSubscriptions) {

                    if (ymlSubscription.getOverrideHttps() && camelConfiguration.getInboundUrl().startsWith("https://")) {
                        ymlSubscription.setAddress(camelConfiguration.getInboundUrl().replaceFirst("https:", "http:"));
                    } else {
                        ymlSubscription.setAddress(camelConfiguration.getInboundUrl());
                    }
                    Subscription subscription = repository.save(createSubscription(ymlSubscription));
                    subscriptions.add(subscription);
                }
            }
//            List<SubscriptionSetup> subscriptions = subscriptionConfig.getSubscriptions();
            logger.info("Initializing {} subscriptions", subscriptions.size());
            Set<String> subscriptionIds = new HashSet<>();

            List<Subscription> actualSubscriptionSetups = new ArrayList<>();

            // Validation and consistency-verification
            for (Subscription subscription : subscriptions) {

                if (subscription.isOverrideHttps() && camelConfiguration.getInboundUrl().startsWith("https://")) {
                    subscription.setAddress(camelConfiguration.getInboundUrl().replaceFirst("https:", "http:"));
                } else {
                    subscription.setAddress(camelConfiguration.getInboundUrl());
                }

                if (!isValid(subscription)) {
                    throw new ServiceConfigurationError("Configuration is not valid for subscription " + subscription);
                }

                if (subscriptionIds.contains(subscription.getSubscriptionId())) {
                    //Verify subscriptionId-uniqueness
                    throw new ServiceConfigurationError("SubscriptionIds are NOT unique for ID="+subscription.getSubscriptionId());
                }


                if (mappingAdaptersById.containsKey(subscription.getMappingAdapterId())) {
                    Class adapterClass = mappingAdaptersById.get(subscription.getMappingAdapterId());
                    try {
                        List<ValueAdapter> valueAdapters = (List<ValueAdapter>) adapterClass.getMethod("getValueAdapters", Subscription.class).invoke(adapterClass.newInstance(), subscription);
                        subscription.getMappingAdapters().addAll(valueAdapters);
                    } catch (Exception e) {
                        throw new ServiceConfigurationError("Invalid mappingAdapterId for subscription " + subscription);
                    }
                }


                if (subscription.getSubscriptionMode() == SubscriptionMode.FETCHED_DELIVERY) {

                    //Fetched delivery needs both subscribe-route and ServiceRequest-route
                    String url = subscription.getUrlMap().get(RequestType.SUBSCRIBE);

                    subscription.getUrlMap().putIfAbsent(RequestType.GET_ESTIMATED_TIMETABLE, url);
                    subscription.getUrlMap().putIfAbsent(RequestType.GET_VEHICLE_MONITORING, url);
                    subscription.getUrlMap().putIfAbsent(RequestType.GET_SITUATION_EXCHANGE, url);
                }

                actualSubscriptionSetups.add(subscription);
                subscriptionIds.add(subscription.getSubscriptionId());

            }

            for (Subscription subscription : actualSubscriptionSetups) {

                try {
                    if (subscription.getSubscriptionMode() == SubscriptionMode.FETCHED_DELIVERY) {

                        subscription.setSubscriptionMode(SubscriptionMode.SUBSCRIBE);
                        camelContext.addRoutes(getRouteBuilder(subscription));

                        subscription.setSubscriptionMode(SubscriptionMode.FETCHED_DELIVERY);
                        camelContext.addRoutes(getRouteBuilder(subscription));

                    } else {

                        RouteBuilder routeBuilder = getRouteBuilder(subscription);
                        //Adding all routes to current context
                        camelContext.addRoutes(routeBuilder);
                    }

                } catch (Exception e) {
                    logger.warn("Could not add subscription", e);
                }
            }

            for (Subscription subscription : actualSubscriptionSetups) {
                if (!subscriptionManager.isSubscriptionRegistered(subscription.getSubscriptionId())) {
                    subscriptionManager.addSubscription(subscription);
                }
            }
        } else {
            logger.error("Subscriptions not configured correctly - no subscriptions will be started");
        }

    }

    private Subscription createSubscription(YamlSubscriptionSetup ymlSubscription) {
        Subscription subscription = new Subscription();
        subscription.setActive(ymlSubscription.isActive());
        subscription.setVendor(ymlSubscription.getVendor());
        subscription.setAddressFieldName(ymlSubscription.getAddressFieldName());
        subscription.setUrlMap(ymlSubscription.getUrlMap());

        subscription.setSubscriptionType(ymlSubscription.getSubscriptionType());
        subscription.setServiceType(ymlSubscription.getServiceType());

        subscription.setSubscriptionMode(ymlSubscription.getSubscriptionMode());
        if (ymlSubscription.filterMapPresets != null) {
            subscription.setFilterMapPreset(ymlSubscription.filterMapPresets);
        }

        subscription.setHeartbeatInterval(ymlSubscription.getHeartbeatInterval());
        subscription.setUpdateInterval(ymlSubscription.getUpdateInterval());
        subscription.setPreviewInterval(ymlSubscription.getPreviewInterval());
        subscription.setChangeBeforeUpdates(ymlSubscription.getChangeBeforeUpdates());
        subscription.setDurationOfSubscription(ymlSubscription.getDurationOfSubscription());
        subscription.setOperatorNamespace(ymlSubscription.getOperatorNamespace());

        subscription.setSubscriptionId(ymlSubscription.getSubscriptionId());
        subscription.setVersion(ymlSubscription.getVersion());
        subscription.setDatasetId(ymlSubscription.getDatasetId());
        subscription.setRequestorRef(ymlSubscription.getRequestorRef());
        subscription.setIdMappingPrefixes(ymlSubscription.getIdMappingPrefixes());
        subscription.setMappingAdapterId(ymlSubscription.getMappingAdapterId());
        subscription.setAddressFieldName(ymlSubscription.getAddressFieldName());
        subscription.setSoapenvNamespace(ymlSubscription.getSoapenvNamespace());
        subscription.setIncrementalUpdates(ymlSubscription.getIncrementalUpdates());
        subscription.setOverrideHttps(ymlSubscription.getOverrideHttps());
        subscription.setContentType(ymlSubscription.getContentType());
        subscription.setVehicleMonitoringRefValue(ymlSubscription.getVehicleMonitoringRefValue());

        return subscription;
    }

    private RouteBuilder getRouteBuilder(Subscription subscription) {
        RouteBuilder route;
        if (subscription.getVersion().equals("1.4")) {
            if (subscription.getSubscriptionMode() == SubscriptionMode.SUBSCRIBE) {
                if (subscription.getServiceType() == ServiceType.SOAP) {
                    route = new Siri20ToSiriWS14Subscription(camelConfiguration, handler, subscription, subscriptionManager);
                } else {
                    route = new Siri20ToSiriRS14Subscription(camelConfiguration, handler, subscription, subscriptionManager);
                }
            } else {
                route = new Siri20ToSiriWS14RequestResponse(camelConfiguration, subscription, subscriptionManager);
            }
        } else {
            if (subscription.getSubscriptionMode() == SubscriptionMode.SUBSCRIBE) {
                if (subscription.getServiceType() == ServiceType.SOAP) {
                    route = new Siri20ToSiriWS20Subscription(camelConfiguration, handler, subscription, subscriptionManager);
                } else {
                    route = new Siri20ToSiriRS20Subscription(camelConfiguration, handler, subscription, subscriptionManager);
                }
            } else {
                route = new Siri20ToSiriRS20RequestResponse(camelConfiguration, subscription, subscriptionManager);
            }
        }
        return route;
    }

    private boolean isValid(Subscription s) {
        Preconditions.checkNotNull(s.getVendor(), "Vendor is not set");
        Preconditions.checkNotNull(s.getDatasetId(), "DatasetId is not set");
        Preconditions.checkNotNull(s.getServiceType(), "ServiceType is not set");
        Preconditions.checkNotNull(s.getSubscriptionType(), "SubscriptionType is not set");
        Preconditions.checkNotNull(s.getVersion(), "Version is not set");
        Preconditions.checkNotNull(s.getSubscriptionId(), "SubscriptionId is not set");
        Preconditions.checkNotNull(s.getRequestorRef(), "RequestorRef is not set");
        Preconditions.checkNotNull(s.getSubscriptionMode(), "SubscriptionMode is not set");
        Preconditions.checkNotNull(s.getContentType(), "ContentType is not set");

        Preconditions.checkNotNull(s.getDurationOfSubscription(), "Duration is not set");
        Preconditions.checkState(s.getDurationOfSubscription().toMillis() > 0, "Duration must be > 0");

        Preconditions.checkNotNull(s.getHeartbeatInterval(), "HeartbeatInterval is not set");
        Preconditions.checkState(s.getHeartbeatInterval().toMillis() > 0, "HeartbeatInterval must be > 0");

        Preconditions.checkNotNull(s.getUrlMap(), "UrlMap is not set");
        Map<RequestType, String> urlMap = s.getUrlMap();
        if (s.getSubscriptionMode() == SubscriptionMode.REQUEST_RESPONSE) {

            if (SubscriptionType.SITUATION_EXCHANGE.equals(s.getSubscriptionType())) {
                Preconditions.checkNotNull(urlMap.get(RequestType.GET_SITUATION_EXCHANGE), "GET_SITUATION_EXCHANGE-url is missing. " + s);
            } else if (SubscriptionType.VEHICLE_MONITORING.equals(s.getSubscriptionType())) {
                Preconditions.checkNotNull(urlMap.get(RequestType.GET_VEHICLE_MONITORING), "GET_VEHICLE_MONITORING-url is missing. " + s);
            } else if (SubscriptionType.ESTIMATED_TIMETABLE.equals(s.getSubscriptionType())) {
                Preconditions.checkNotNull(urlMap.get(RequestType.GET_ESTIMATED_TIMETABLE), "GET_ESTIMATED_TIMETABLE-url is missing. " + s);
            } else {
                Preconditions.checkArgument(false, "URLs not configured correctly");
            }
        } else if (s.getSubscriptionMode() == SubscriptionMode.SUBSCRIBE) {

            //Type-specific requirements
            if (SubscriptionType.ESTIMATED_TIMETABLE.equals(s.getSubscriptionType())) {
                Preconditions.checkNotNull(s.getPreviewInterval(), "PreviewInterval is not set");
            } else if (SubscriptionType.SITUATION_EXCHANGE.equals(s.getSubscriptionType())) {
                Preconditions.checkNotNull(s.getPreviewInterval(), "PreviewInterval is not set");
            }

            Preconditions.checkNotNull(urlMap.get(RequestType.SUBSCRIBE), "SUBSCRIBE-url is missing. " + s);
            Preconditions.checkNotNull(urlMap.get(RequestType.DELETE_SUBSCRIPTION), "DELETE_SUBSCRIPTION-url is missing. " + s);
        }  else if (s.getSubscriptionMode() == SubscriptionMode.FETCHED_DELIVERY) {
            Preconditions.checkNotNull(urlMap.get(RequestType.SUBSCRIBE), "SUBSCRIBE-url is missing. " + s);
            Preconditions.checkNotNull(urlMap.get(RequestType.DELETE_SUBSCRIPTION), "DELETE_SUBSCRIPTION-url is missing. " + s);
        } else {
            Preconditions.checkArgument(false, "Subscription mode not configured");
        }

        return true;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
