package no.rutebanken.anshar.subscription.models;

import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import no.rutebanken.anshar.subscription.DurationConverter;
import no.rutebanken.anshar.subscription.RequestType;
import no.rutebanken.anshar.subscription.SubscriptionPreset;
import no.rutebanken.anshar.subscription.SubscriptionSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
public class Subscription implements Serializable {
    @Transient
    private Logger logger = LoggerFactory.getLogger(Subscription.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long internalId;

    @Transient
    private List<ValueAdapter> mappingAdapters = new ArrayList<>();

    @OneToOne
    private Activity activity;

    @Enumerated(EnumType.STRING)
    private SubscriptionSetup.SubscriptionType subscriptionType;

    @Enumerated(EnumType.STRING)
    private SubscriptionSetup.ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    private SubscriptionSetup.SubscriptionMode subscriptionMode;

    @Enumerated(EnumType.STRING)
    private SubscriptionPreset filterMapPreset;


    @Convert(converter = DurationConverter.class)
    private Duration heartbeatInterval;

    @Convert(converter = DurationConverter.class)
    private Duration updateInterval;

    @Convert(converter = DurationConverter.class)
    private Duration previewInterval;

    @Convert(converter = DurationConverter.class)
    private Duration changeBeforeUpdates;

    @Convert(converter = DurationConverter.class)
    private Duration durationOfSubscription;

    private String operatorNamespace;

    private String subscriptionId;
    private String version;
    private String vendor;
    private String datasetId;

    private String requestorRef;
    private boolean active;


    @ElementCollection
    private List<String> idMappingPrefixes;

    private String mappingAdapterId;


    private String addressFieldName;
    private String soapenvNamespace;
    private Boolean incrementalUpdates;
    private boolean overrideHttps;
    private String contentType;
    private String vehicleMonitoringRefValue;


    @MapKeyColumn(name = "Request_Type")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "URL")
    @ElementCollection(fetch = FetchType.EAGER)
    private Map<RequestType, String> urlMap;

    public long getInternalId() {
        return internalId;
    }

    public void setInternalId(long internalId) {
        this.internalId = internalId;
    }

    public List<ValueAdapter> getMappingAdapters() {
        return mappingAdapters;
    }

    public void setMappingAdapters(List<ValueAdapter> mappingAdapters) {
        this.mappingAdapters = mappingAdapters;
    }

    public SubscriptionSetup.SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(SubscriptionSetup.SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public SubscriptionSetup.ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(SubscriptionSetup.ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public SubscriptionSetup.SubscriptionMode getSubscriptionMode() {
        return subscriptionMode;
    }

    public void setSubscriptionMode(SubscriptionSetup.SubscriptionMode subscriptionMode) {
        this.subscriptionMode = subscriptionMode;
    }

    public SubscriptionPreset getFilterMapPreset() {
        return filterMapPreset;
    }

    public void setFilterMapPreset(SubscriptionPreset filterMapPreset) {
        this.filterMapPreset = filterMapPreset;
    }

    public Duration getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(Duration heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public Duration getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(Duration updateInterval) {
        this.updateInterval = updateInterval;
    }

    public Duration getPreviewInterval() {
        return previewInterval;
    }

    public void setPreviewInterval(Duration previewInterval) {
        this.previewInterval = previewInterval;
    }

    public Duration getChangeBeforeUpdates() {
        return changeBeforeUpdates;
    }

    public void setChangeBeforeUpdates(Duration changeBeforeUpdates) {
        this.changeBeforeUpdates = changeBeforeUpdates;
    }

    public Duration getDurationOfSubscription() {
        return durationOfSubscription;
    }

    public void setDurationOfSubscription(Duration durationOfSubscription) {
        this.durationOfSubscription = durationOfSubscription;
    }

    public String getOperatorNamespace() {
        return operatorNamespace;
    }

    public void setOperatorNamespace(String operatorNamespace) {
        this.operatorNamespace = operatorNamespace;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getRequestorRef() {
        return requestorRef;
    }

    public void setRequestorRef(String requestorRef) {
        this.requestorRef = requestorRef;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<String> getIdMappingPrefixes() {
        return idMappingPrefixes;
    }

    public void setIdMappingPrefixes(List<String> idMappingPrefixes) {
        this.idMappingPrefixes = idMappingPrefixes;
    }

    public String getMappingAdapterId() {
        return mappingAdapterId;
    }

    public void setMappingAdapterId(String mappingAdapterId) {
        this.mappingAdapterId = mappingAdapterId;
    }

    public String getAddressFieldName() {
        return addressFieldName;
    }

    public void setAddressFieldName(String addressFieldName) {
        this.addressFieldName = addressFieldName;
    }

    public String getSoapenvNamespace() {
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

    public boolean isOverrideHttps() {
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

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public String toString() {
        return MessageFormat.format("[vendor={0}, subscriptionId={1}, internalId={2}]", vendor, subscriptionId, internalId);
    }

    /**
     * Variant of equals that only compares fields crucial to detect updated subscription-config
     * NOTE: e.g. subscriptionId is NOT compared
     *
     * @param o
     * @return true if crucial config-elements are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Subscription)) {
            return false;
        }

        Subscription that = (Subscription) o;

        if (getInternalId() != that.getInternalId()) {
            logger.info("getInternalId() does not match [{}] vs [{}]", getInternalId(), that.getInternalId());
            return false;
        }
        if (getSubscriptionType() != that.getSubscriptionType()) {
            logger.info("getSubscriptionType() does not match [{}] vs [{}]", getSubscriptionType(), that.getSubscriptionType());
            return false;
        }
        if (getOperatorNamespace() != null ? !getOperatorNamespace().equals(that.getOperatorNamespace()) : that.getOperatorNamespace() != null) {
            logger.info("getOperatorNamespace() does not match [{}] vs [{}]", getOperatorNamespace(), that.getOperatorNamespace());
            return false;
        }
        if (!getVersion().equals(that.getVersion())) {
            logger.info("getVersion() does not match [{}] vs [{}]", getVersion(), that.getVersion());
            return false;
        }
        if (!getVendor().equals(that.getVendor())) {
            logger.info("getVendor() does not match [{}] vs [{}]", getVendor(), that.getVendor());
            return false;
        }
        if (!getDatasetId().equals(that.getDatasetId())) {
            logger.info("getDatasetId() does not match [{}] vs [{}]", getDatasetId(), that.getDatasetId());
            return false;
        }
        if (getServiceType() != that.getServiceType()) {
            logger.info("getServiceType() does not match [{}] vs [{}]", getServiceType(), that.getServiceType());
            return false;
        }
        if (getDurationOfSubscription() != null ? !getDurationOfSubscription().equals(that.getDurationOfSubscription()) : that.getDurationOfSubscription() != null) {
            logger.info("getDurationOfSubscription() does not match [{}] vs [{}]", getDurationOfSubscription(), that.getDurationOfSubscription());
            return false;
        }
        if (getSubscriptionMode() != that.getSubscriptionMode()) {
            logger.info("getSubscriptionMode() does not match [{}] vs [{}]", getSubscriptionMode(), that.getSubscriptionMode());
            return false;
        }
        if (getIdMappingPrefixes() != null ? !getIdMappingPrefixes().equals(that.getIdMappingPrefixes()) : that.getIdMappingPrefixes() != null) {
            logger.info("getIdMappingPrefixes() does not match [{}] vs [{}]", getIdMappingPrefixes(), that.getIdMappingPrefixes());
            return false;
        }
        if (getMappingAdapterId() != null ? !getMappingAdapterId().equals(that.getMappingAdapterId()) : that.getMappingAdapterId() != null) {
            logger.info("getMappingAdapterId() does not match [{}] vs [{}]", getMappingAdapterId(), that.getMappingAdapterId());
            return false;
        }
        return true;
    }

    public void setUrlMap(Map<RequestType, String> urlMap) {
        this.urlMap = urlMap;
    }

    public Map<RequestType, String> getUrlMap() {
        return urlMap;
    }
}
