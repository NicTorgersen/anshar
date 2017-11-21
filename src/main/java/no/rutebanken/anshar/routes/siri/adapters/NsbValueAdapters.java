package no.rutebanken.anshar.routes.siri.adapters;

import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import no.rutebanken.anshar.routes.siri.transformer.impl.LeftPaddingAdapter;
import no.rutebanken.anshar.subscription.models.Subscription;
import uk.org.ifopt.siri20.StopPlaceRef;
import uk.org.siri.siri20.StopPointRef;

import java.util.ArrayList;
import java.util.List;

@Mapping(id="nsb")
public class NsbValueAdapters extends MappingAdapter {


    @Override
    public List<ValueAdapter> getValueAdapters(Subscription subscription) {

        List<ValueAdapter> valueAdapters = new ArrayList<>();

        valueAdapters.add(new LeftPaddingAdapter(StopPointRef.class, 9, '0'));
        valueAdapters.add(new LeftPaddingAdapter(StopPlaceRef.class, 9, '0'));

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
