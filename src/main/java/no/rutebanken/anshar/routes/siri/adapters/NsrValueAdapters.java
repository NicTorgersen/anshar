package no.rutebanken.anshar.routes.siri.adapters;

import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import no.rutebanken.anshar.subscription.models.Subscription;

import java.util.ArrayList;
import java.util.List;

@Mapping(id="nsr")
public class NsrValueAdapters extends MappingAdapter {


    @Override
    public List<ValueAdapter> getValueAdapters(Subscription subscription) {

        List<ValueAdapter> valueAdapters = new ArrayList<>();

        valueAdapters.addAll(createNsrIdMappingAdapters(subscription.getIdMappingPrefixes()));

        if (subscription.getDatasetId() != null && !subscription.getDatasetId().isEmpty()) {
            List<ValueAdapter> datasetPrefix = createIdPrefixAdapters(subscription.getDatasetId());
            if (!subscription.getMappingAdapters().containsAll(datasetPrefix)) {
                subscription.getMappingAdapters().addAll(datasetPrefix);
            }
        }

        return valueAdapters;
    }
}
