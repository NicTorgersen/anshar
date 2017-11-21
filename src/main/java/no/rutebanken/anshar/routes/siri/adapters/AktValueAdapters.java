package no.rutebanken.anshar.routes.siri.adapters;

import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import no.rutebanken.anshar.routes.siri.transformer.impl.LeftPaddingAdapter;
import no.rutebanken.anshar.subscription.models.Subscription;
import uk.org.siri.siri20.LineRef;

import java.util.ArrayList;
import java.util.List;

@Mapping(id="akt")
public class AktValueAdapters extends MappingAdapter {


    @Override
    public List<ValueAdapter> getValueAdapters(Subscription subscription) {

        List<ValueAdapter> valueAdapters = new ArrayList<>();
        valueAdapters.add(new LeftPaddingAdapter(LineRef.class, 4, '0'));

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
