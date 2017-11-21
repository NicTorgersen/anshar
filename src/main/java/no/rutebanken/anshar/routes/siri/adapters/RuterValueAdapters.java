package no.rutebanken.anshar.routes.siri.adapters;

import no.rutebanken.anshar.routes.siri.processor.RuterDatedVehicleRefPostProcessor;
import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import no.rutebanken.anshar.routes.siri.transformer.impl.LeftPaddingAdapter;
import no.rutebanken.anshar.routes.siri.transformer.impl.RuterSubstringAdapter;
import no.rutebanken.anshar.subscription.models.Subscription;
import uk.org.ifopt.siri20.StopPlaceRef;
import uk.org.siri.siri20.DestinationRef;
import uk.org.siri.siri20.JourneyPlaceRefStructure;
import uk.org.siri.siri20.StopPointRef;

import java.util.ArrayList;
import java.util.List;

@Mapping(id="ruter")
public class RuterValueAdapters extends MappingAdapter {


    @Override
    public List<ValueAdapter> getValueAdapters(Subscription subscription) {

        List<ValueAdapter> valueAdapters = new ArrayList<>();
                valueAdapters.add(new LeftPaddingAdapter(StopPlaceRef.class, 8, '0'));
                valueAdapters.add(new RuterSubstringAdapter(StopPointRef.class, ':', '0', 2));
                valueAdapters.add(new RuterSubstringAdapter(JourneyPlaceRefStructure.class, ':', '0', 2));
                valueAdapters.add(new RuterSubstringAdapter(DestinationRef.class, ':', '0', 2));
                valueAdapters.add(new RuterDatedVehicleRefPostProcessor());

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
