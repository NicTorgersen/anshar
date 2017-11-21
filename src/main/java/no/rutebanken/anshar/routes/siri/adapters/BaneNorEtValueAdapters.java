package no.rutebanken.anshar.routes.siri.adapters;

import no.rutebanken.anshar.routes.siri.processor.BaneNorIdPlatformPostProcessor;
import no.rutebanken.anshar.routes.siri.processor.OperatorFilterPostProcessor;
import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import no.rutebanken.anshar.subscription.models.Subscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mapping(id="banenoret")
public class BaneNorEtValueAdapters extends MappingAdapter {


    @Override
    public List<ValueAdapter> getValueAdapters(Subscription subscription) {

        List<ValueAdapter> valueAdapters = new ArrayList<>();
        valueAdapters.add(new BaneNorIdPlatformPostProcessor());

        valueAdapters.add(new OperatorFilterPostProcessor(Arrays.asList("BN")));

        return valueAdapters;
    }
}
