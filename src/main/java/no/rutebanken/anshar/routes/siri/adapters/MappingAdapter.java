package no.rutebanken.anshar.routes.siri.adapters;

import no.rutebanken.anshar.routes.siri.handlers.OutboundIdMappingPolicy;
import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import no.rutebanken.anshar.routes.siri.transformer.impl.PrefixAdapter;
import no.rutebanken.anshar.routes.siri.transformer.impl.StopPlaceRegisterMapper;
import no.rutebanken.anshar.subscription.MappingAdapterPresets;
import no.rutebanken.anshar.subscription.models.Subscription;
import uk.org.ifopt.siri20.StopPlaceRef;
import uk.org.siri.siri20.*;

import java.util.ArrayList;
import java.util.List;

public abstract class MappingAdapter {

    public abstract List<ValueAdapter> getValueAdapters(Subscription subscription);

    public List<ValueAdapter> getOutboundValueAdapters(OutboundIdMappingPolicy mappingPolicy) {
        return new MappingAdapterPresets().getOutboundAdapters(mappingPolicy);
    }
    public List<ValueAdapter> createNsrIdMappingAdapters(List<String> idMappingPrefixes) {
        List<ValueAdapter> nsr = new ArrayList<>();
        nsr.add(new StopPlaceRegisterMapper(StopPlaceRef.class, idMappingPrefixes, "StopPlace"));
        nsr.add(new StopPlaceRegisterMapper(StopPointRef.class, idMappingPrefixes));
        nsr.add(new StopPlaceRegisterMapper(JourneyPlaceRefStructure.class, idMappingPrefixes));
        nsr.add(new StopPlaceRegisterMapper(DestinationRef.class, idMappingPrefixes));
        return nsr;
    }


    public List<ValueAdapter> createIdPrefixAdapters(String datasetId) {
        List<ValueAdapter> adapters = new ArrayList<>();
        adapters.add(new PrefixAdapter(LineRef.class, datasetId + ":Line:"));
        adapters.add(new PrefixAdapter(CourseOfJourneyRefStructure.class, datasetId + ":VehicleJourney:"));
        return adapters;
    }
}
