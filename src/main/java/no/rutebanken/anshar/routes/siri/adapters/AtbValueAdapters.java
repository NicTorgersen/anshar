package no.rutebanken.anshar.routes.siri.adapters;

import no.rutebanken.anshar.routes.siri.transformer.ValueAdapter;
import no.rutebanken.anshar.subscription.models.Subscription;

import java.util.List;

@Mapping(id="atb")
public class AtbValueAdapters extends AktValueAdapters {


    @Override
    public List<ValueAdapter> getValueAdapters(Subscription subscription) {
        return super.getValueAdapters(subscription);
    }
}
