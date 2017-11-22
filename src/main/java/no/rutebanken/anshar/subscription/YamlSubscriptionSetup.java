package no.rutebanken.anshar.subscription;

import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import no.rutebanken.anshar.subscription.enums.ServiceType;
import no.rutebanken.anshar.subscription.enums.SubscriptionMode;
import no.rutebanken.anshar.subscription.enums.SubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.*;

public class YamlSubscriptionSetup implements Serializable {

    private Logger logger = LoggerFactory.getLogger(YamlSubscriptionSetup.class);
    private long internalId;
    private List<ValueAdapter> mappingAdapters = new ArrayList<>();
    private SubscriptionType subscriptionType;
    private String address;
    private Duration heartbeatInterval;
    private Duration updateInterval;
    private Duration previewInterval;
    private Duration changeBeforeUpdates;
    private String operatorNamespace;
    private Map<RequestType, String> urlMap;
    private String subscriptionId;
    private String version;
    private String vendor;
    private String datasetId;
    private ServiceType serviceType;
    private Duration durationOfSubscription;
    private String requestorRef;
    private boolean active;
    private SubscriptionMode subscriptionMode;
    private Map<Class, Set<Object>> filterMap;
    private List<String> idMappingPrefixes;
    private String mappingAdapterId;
    SubscriptionPreset filterMapPresets;
    private String addressFieldName;
    private String soapenvNamespace;
    private Boolean incrementalUpdates;
    private boolean overrideHttps;
    private String contentType;
    private String vehicleMonitoringRefValue;

    public YamlSubscriptionSetup() {
    }

    /**
     * @param subscriptionType SX, VM, ET
     * @param address Base-URL for receiving incoming data
     * @param heartbeatInterval Requested heartbeatinterval for subscriptions, Request-interval for Request/Response "subscriptions"
     * @param operatorNamespace Namespace
     * @param urlMap Operation-names and corresponding URL's
     * @param version SIRI-version to use
     * @param vendor Vendorname - information only
     * @param serviceType SOAP/REST
     * @param filterMap
     * @param subscriptionId Sets the subscriptionId to use
     * @param requestorRef
     * @param durationOfSubscription Initial duration of subscription
     * @param active Activates/deactivates subscription
     */
    public YamlSubscriptionSetup(SubscriptionType subscriptionType, SubscriptionMode subscriptionMode, String address, Duration heartbeatInterval, Duration updateInterval, String operatorNamespace, Map<RequestType, String> urlMap,
                             String version, String vendor, String datasetId, ServiceType serviceType, List<ValueAdapter> mappingAdapters, Map<Class, Set<Object>> filterMap, List<String> idMappingPrefixes,
                             String subscriptionId, String requestorRef, Duration durationOfSubscription, boolean active) {
        this.subscriptionType = subscriptionType;
        this.subscriptionMode = subscriptionMode;
        this.address = address;
        this.heartbeatInterval = heartbeatInterval;
        this.updateInterval = updateInterval;
        this.operatorNamespace = operatorNamespace;
        this.urlMap = urlMap;
        this.version = version;
        this.vendor = vendor;
        this.datasetId = datasetId;
        this.serviceType = serviceType;
        this.mappingAdapters = mappingAdapters;
        this.filterMap = filterMap;
        this.idMappingPrefixes = idMappingPrefixes;
        this.subscriptionId = subscriptionId;
        this.requestorRef = requestorRef;
        this.durationOfSubscription = durationOfSubscription;
        this.active = active;
    }

    private String getRouteName(String prefix) {
        return prefix + subscriptionId;
    }

    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public String getOperatorNamespace() {
        return operatorNamespace;
    }

    public Map<RequestType, String> getUrlMap() {
        return urlMap;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getVersion() {
        return version;
    }

    public String getVendor() {
        return vendor;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public List<ValueAdapter> getMappingAdapters() {
        return mappingAdapters;
    }

    public Duration getDurationOfSubscription() {
        return durationOfSubscription;
    }

    public String getRequestorRef() {
        return requestorRef;
    }

    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String toString() {
        return MessageFormat.format("[vendor={0}, subscriptionId={1}, internalId={2}]", vendor, subscriptionId, internalId);
    }

    public long getInternalId() {
        return internalId;
    }

    public void setInternalId(long internalId) {
        this.internalId = internalId;
    }

    public void setSubscriptionMode(SubscriptionMode subscriptionMode) {
        this.subscriptionMode = subscriptionMode;
    }

    public SubscriptionMode getSubscriptionMode() {
        return subscriptionMode;
    }

    public void setFilterPresets(SubscriptionPreset preset) {
        this.filterMapPresets = preset;
        filterMap = new HashMap<>();
        if (preset != null) {
            addFilterMap(new FilterMapPresets().get(preset));
        }
    }
    public void setFilterMap(Map<Class, Set<Object>> filterMap) {
        this.filterMap = filterMap;
    }

    public Map<Class, Set<Object>> getFilterMap() {
        return filterMap;
    }

    private void addFilterMap(Map<Class, Set<Object>> filters) {
        if (this.filterMap == null) {
            this.filterMap = new HashMap<>();
        }
        this.filterMap.putAll(filters);
    }

    public Duration getPreviewInterval() {
        return previewInterval;
    }

    public String getAddressFieldName() {
        if (addressFieldName != null && addressFieldName.isEmpty()) {
            return null;
        }
        return addressFieldName;
    }

    public void setAddressFieldName(String addressFieldName) {
        this.addressFieldName = addressFieldName;
    }

    public String getSoapenvNamespace() {
        if (soapenvNamespace != null && soapenvNamespace.isEmpty()) {
            return null;
        }
        return soapenvNamespace;
    }

    public void setSoapenvNamespace(String soapenvNamespace) {
        this.soapenvNamespace = soapenvNamespace;
    }

    public Boolean getIncrementalUpdates() {
        return incrementalUpdates;
    }

    public void setIncrementalUpdates(Boolean incrementalUpdates) {
        this.incrementalUpdates = incrementalUpdates;
    }

    public boolean getOverrideHttps() {
        return overrideHttps;
    }

    public void setOverrideHttps(boolean overrideHttps) {
        this.overrideHttps = overrideHttps;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getVehicleMonitoringRefValue() {
        return vehicleMonitoringRefValue;
    }

    public void setVehicleMonitoringRefValue(String vehicleMonitoringRefValue) {
        this.vehicleMonitoringRefValue = vehicleMonitoringRefValue;
    }

    public Duration getChangeBeforeUpdates() {
        return changeBeforeUpdates;
    }

    public void setIdMappingPrefixes(List<String> idMappingPrefixes) {
        this.idMappingPrefixes = idMappingPrefixes;
    }

    public List<String> getIdMappingPrefixes() {
        return idMappingPrefixes;
    }

    public String getMappingAdapterId() {
        return mappingAdapterId;
    }

    public void setMappingAdapterId(String mappingAdapterId) {
        this.mappingAdapterId = mappingAdapterId;
    }

    public void setSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public void setAddress(String address) {
        if (address.endsWith("/")) {
            address = address.substring(0, address.length()-1);
        }
        this.address = address;
    }

    public void setHeartbeatIntervalSeconds(int seconds) {
        if (seconds > 0) {
            setHeartbeatInterval(Duration.ofSeconds(seconds));
        }
    }

    void setHeartbeatInterval(Duration heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public Duration getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(Duration updateInterval) {
        this.updateInterval = updateInterval;
    }

    public void setUpdateIntervalSeconds(int seconds) {
        setUpdateInterval(Duration.ofSeconds(seconds));
    }

    public void setPreviewIntervalSeconds(int seconds) {
        setPreviewInterval(Duration.ofSeconds(seconds));
    }

    void setPreviewInterval(Duration previewIntervalSeconds) {
        this.previewInterval = previewIntervalSeconds;
    }
    public void setChangeBeforeUpdatesSeconds(int seconds) {
        setChangeBeforeUpdates(Duration.ofSeconds(seconds));
    }

    void setChangeBeforeUpdates(Duration changeBeforeUpdates) {
        this.changeBeforeUpdates = changeBeforeUpdates;
    }

    public void setOperatorNamespace(String operatorNamespace) {
        this.operatorNamespace = operatorNamespace;
    }

    public void setUrlMap(Map<RequestType, String> urlMap) {
        this.urlMap = urlMap;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public void setDurationOfSubscriptionHours(int hours) {
        this.durationOfSubscription = Duration.ofHours(hours);
    }

    void setDurationOfSubscription(Duration durationOfSubscription) {
        this.durationOfSubscription = durationOfSubscription;
    }

    public void setRequestorRef(String requestorRef) {
        this.requestorRef = requestorRef;
    }
}
