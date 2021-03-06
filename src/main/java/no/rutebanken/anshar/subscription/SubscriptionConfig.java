/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package no.rutebanken.anshar.subscription;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;
import java.util.stream.Collectors;

@PropertySource(value = "${anshar.subscriptions.config.path}", factory = YamlPropertySourceFactory.class)
@ConfigurationProperties(prefix = "anshar")
@Configuration
public class SubscriptionConfig {

    private List<SubscriptionSetup> subscriptions;

    @Value("${anshar.subscriptions.datatypes.filter:}")
    List<SiriDataType> dataTypes;


    public List<SubscriptionSetup> getSubscriptions() {
        if (dataTypes != null && !dataTypes.isEmpty()) {
            return subscriptions.stream()
                .filter(sub -> dataTypes.contains(sub.getSubscriptionType()))
                .collect(Collectors.toList());
        }
        return subscriptions;
    }

    public void setSubscriptions(List<SubscriptionSetup> subscriptions) {
        this.subscriptions = subscriptions;
    }
}
