package no.rutebanken.anshar.subscription;

import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Subscription implements Serializable {
    @Transient
    private Logger logger = LoggerFactory.getLogger(Subscription.class);

    @Id
    @GeneratedValue
    private long internalId;

    @Transient
    private List<ValueAdapter> mappingAdapters = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private SubscriptionSetup.SubscriptionType subscriptionType;

    @Enumerated(EnumType.STRING)
    private SubscriptionSetup.ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    private SubscriptionSetup.SubscriptionMode subscriptionMode;

    @Enumerated(EnumType.STRING)
    private SubscriptionPreset filterMapPreset;

    private String heartbeatInterval;
    private String updateInterval;
    private String previewInterval;
    private String changeBeforeUpdates;
    private String operatorNamespace;

    String subscribeUrl,
            deleteSubscriptionUrl,
            checkStatusUrl,
            getVehicleMonitoringUrl,
            getSituationExchangeUrl,
            getEstimatedTimetableUrl;

    private String subscriptionId;
    private String version;
    private String vendor;
    private String datasetId;

    private String durationOfSubscription;
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

    public Duration getHeartbeatInterval() {
        return Duration.parse(heartbeatInterval);
    }

    public String getOperatorNamespace() {
        return operatorNamespace;
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

    public SubscriptionSetup.ServiceType getServiceType() {
        return serviceType;
    }

    public List<ValueAdapter> getMappingAdapters() {
        return mappingAdapters;
    }

    public Duration getDurationOfSubscription() {
        return Duration.parse(durationOfSubscription);
    }

    public void setDurationOfSubscription(String durationOfSubscription) {
        this.durationOfSubscription = durationOfSubscription;
    }

    public String getRequestorRef() {
        return requestorRef;
    }

    public SubscriptionSetup.SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getInternalId() {
        return internalId;
    }

    public SubscriptionSetup.SubscriptionMode getSubscriptionMode() {
        return subscriptionMode;
    }

    public SubscriptionPreset getFilterMapPreset() {
        return filterMapPreset;
    }

    public String getUpdateInterval() {
        return updateInterval;
    }

    public String getPreviewInterval() {
        return previewInterval;
    }

    public String getChangeBeforeUpdates() {
        return changeBeforeUpdates;
    }

    public List<String> getIdMappingPrefixes() {
        return idMappingPrefixes;
    }

    public String getMappingAdapterId() {
        return mappingAdapterId;
    }

    public String getAddressFieldName() {
        return addressFieldName;
    }

    public String getSoapenvNamespace() {
        return soapenvNamespace;
    }

    public Boolean getIncrementalUpdates() {
        return incrementalUpdates;
    }

    public boolean isOverrideHttps() {
        return overrideHttps;
    }

    public String getContentType() {
        return contentType;
    }

    public String getVehicleMonitoringRefValue() {
        return vehicleMonitoringRefValue;
    }

    public String getUrl(RequestType requestType) {
        switch (requestType) {
            case SUBSCRIBE:
                return subscribeUrl;
            case CHECK_STATUS:
                return checkStatusUrl;
            case DELETE_SUBSCRIPTION:
                return deleteSubscriptionUrl;
            case GET_SITUATION_EXCHANGE:
                return getSituationExchangeUrl;
            case GET_VEHICLE_MONITORING:
                return getVehicleMonitoringUrl;
            case GET_ESTIMATED_TIMETABLE:
                return getEstimatedTimetableUrl;
            default:
                return  null;
        }
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

        if (subscribeUrl != null ? !subscribeUrl.equals(that.subscribeUrl) : that.subscribeUrl != null) {
            return false;
        }
        if (deleteSubscriptionUrl != null ? !deleteSubscriptionUrl.equals(that.deleteSubscriptionUrl) : that.deleteSubscriptionUrl != null) {
            return false;
        }
        if (checkStatusUrl != null ? !checkStatusUrl.equals(that.checkStatusUrl) : that.checkStatusUrl != null) {
            return false;
        }
        if (getVehicleMonitoringUrl != null ? !getVehicleMonitoringUrl.equals(that.getVehicleMonitoringUrl) : that.getVehicleMonitoringUrl != null) {
            return false;
        }
        if (getSituationExchangeUrl != null ? !getSituationExchangeUrl.equals(that.getSituationExchangeUrl) : that.getSituationExchangeUrl != null) {
            return false;
        }
        if (getEstimatedTimetableUrl != null ? !getEstimatedTimetableUrl.equals(that.getEstimatedTimetableUrl) : that.getEstimatedTimetableUrl != null) {
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
}
