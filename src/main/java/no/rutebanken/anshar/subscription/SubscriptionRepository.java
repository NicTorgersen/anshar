package no.rutebanken.anshar.subscription;

import no.rutebanken.anshar.subscription.models.Subscription;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface SubscriptionRepository extends Repository<Subscription, Long> {

    List<Subscription> findAll();

    Subscription findBySubscriptionId(String subscriptionId);

    Subscription save(Subscription subscription);

}