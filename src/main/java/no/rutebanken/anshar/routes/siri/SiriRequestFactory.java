package no.rutebanken.anshar.routes.siri;

import no.rutebanken.anshar.subscription.enums.SubscriptionMode;
import no.rutebanken.anshar.subscription.models.Subscription;
import uk.org.siri.siri20.Siri;

public class SiriRequestFactory {

	private Subscription subscription;

	public static String getCamelUrl(String url) {
		return getCamelUrl(url, null);
	}
	public static String getCamelUrl(String url, String parameters) {
		if (url != null) {
			if (parameters != null && !parameters.isEmpty()) {
				String separator = "?";
				if (url.contains("?")) {
					separator = "&";
				}
				if (parameters.startsWith("?")) {
					parameters = parameters.substring(1);
				}
				url = url + separator + parameters;
			}

			if (url.startsWith("https4://")) {
				return url;
			}
		}
		return "http4://" + url;
	}

	public SiriRequestFactory(Subscription subscription) {
		this.subscription = subscription;
	}

	/*
	 * Called dynamically from camel-routes
	 */
	public Siri createSiriSubscriptionRequest() {
		return SiriObjectFactory.createSubscriptionRequest(subscription);
	}

	/*
	 * Called dynamically from camel-routes
	 */
	public Siri createSiriTerminateSubscriptionRequest() {
		return SiriObjectFactory.createTerminateSubscriptionRequest(subscription);

	}

	/*
	 * Called dynamically from camel-routes
	 */
	public Siri createSiriCheckStatusRequest() {
		return SiriObjectFactory.createCheckStatusRequest(subscription);

	}

	private Boolean allData = Boolean.TRUE;

	/*
	 * Called dynamically from camel-routes
	 *
	 * Creates ServiceRequest or DataSupplyRequest based on subscription type
	 */
	public Siri createSiriDataRequest() {
		Siri request = null;
		if (subscription.getSubscriptionMode() == SubscriptionMode.FETCHED_DELIVERY) {
			request = SiriObjectFactory.createDataSupplyRequest(subscription, allData);
			allData = Boolean.FALSE;
		} else {
			request = SiriObjectFactory.createServiceRequest(subscription);
		}

		return request;
	}

}
