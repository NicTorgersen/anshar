package no.rutebanken.anshar.routes.siri.adapters;

import no.rutebanken.anshar.routes.siri.processor.OstfoldIdPlatformPostProcessor;
import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import no.rutebanken.anshar.subscription.models.Subscription;

import java.util.ArrayList;
import java.util.List;

@Mapping(id="ostfoldet")
public class OstfoldEtValueAdapters extends MappingAdapter {


    @Override
    public List<ValueAdapter> getValueAdapters(Subscription subscription) {

        List<ValueAdapter> valueAdapters = new ArrayList<>();
        valueAdapters.add(new OstfoldIdPlatformPostProcessor());

        return valueAdapters;
    }
}
