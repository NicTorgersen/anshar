package no.rutebanken.anshar.routes.siri;

import no.rutebanken.anshar.dataformat.SiriDataFormatHelper;
import no.rutebanken.anshar.routes.CamelConfiguration;
import no.rutebanken.anshar.subscription.SubscriptionManager;
import no.rutebanken.anshar.subscription.enums.SubscriptionMode;
import no.rutebanken.anshar.subscription.models.Subscription;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;

import static no.rutebanken.anshar.routes.siri.SiriIncomingReceiver.TRANSFORM_SOAP;
import static no.rutebanken.anshar.routes.siri.SiriIncomingReceiver.TRANSFORM_VERSION;
import static no.rutebanken.anshar.subscription.SubscriptionHelper.*;

public class Siri20ToSiriWS14RequestResponse extends SiriSubscriptionRouteBuilder {

    public Siri20ToSiriWS14RequestResponse(CamelConfiguration config, Subscription subscription, SubscriptionManager subscriptionManager) {
        super(config, subscriptionManager);

        this.subscription = subscription;
    }

    @Override
    public void configure() throws Exception {

        long heartbeatIntervalMillis = subscription.getHeartbeatInterval().toMillis();

        SiriRequestFactory helper = new SiriRequestFactory(subscription);

        String httpOptions = getTimeout();

        if (subscription.getSubscriptionMode() == SubscriptionMode.REQUEST_RESPONSE) {
            singletonFrom("quartz2://anshar/monitor_" + getRequestResponseRouteName(subscription) + "?fireNow=true&trigger.repeatInterval=" + heartbeatIntervalMillis,
                    "monitor.ws.14." + subscription.getSubscriptionType() + "." + subscription.getVendor())
                    .choice()
                    .when(p -> requestData(subscription.getSubscriptionId(), p.getFromRouteId()))
                    .to("direct:" + getServiceRequestRouteName(subscription))
                    .endChoice()
            ;
        }

        from("direct:" + getServiceRequestRouteName(subscription))
                .log("Retrieving data " + subscription.toString())
                .bean(helper, "createSiriDataRequest", false)
                .marshal(SiriDataFormatHelper.getSiriJaxbDataformat())
                .setExchangePattern(ExchangePattern.InOut) // Make sure we wait for a response
                .setHeader("SOAPAction", simple(getSoapAction(subscription))) // extract and compute SOAPAction (Microsoft requirement)
                .setHeader("operatorNamespace", constant(subscription.getOperatorNamespace())) // Need to make SOAP request with endpoint specific element namespace
                .to("xslt:xsl/siri_20_14.xsl") // Convert SIRI raw request to SOAP version
                .to("xslt:xsl/siri_raw_soap.xsl") // Convert SIRI raw request to SOAP version
                .removeHeaders("CamelHttp*") // Remove any incoming HTTP headers as they interfere with the outgoing definition
                .setHeader(Exchange.CONTENT_TYPE, constant(subscription.getContentType())) // Necessary when talking to Microsoft web services
                .setHeader(Exchange.HTTP_METHOD, constant(org.apache.camel.component.http4.HttpMethods.POST))
                .to("log:request:" + getClass().getSimpleName() + "?showAll=true&multiline=true")
                .to(getRequestUrl(subscription) + httpOptions)
                .setHeader("CamelHttpPath", constant("/appContext" + buildUrl(subscription, false)))
                .log("Got response " + subscription.toString())
                //.to("log:response:" + getClass().getSimpleName() + "?showAll=true&multiline=true")
                .setHeader(TRANSFORM_VERSION, constant(TRANSFORM_VERSION))
                .setHeader(TRANSFORM_SOAP, constant(TRANSFORM_SOAP))
                .to("activemq:queue:" + CamelConfiguration.TRANSFORM_QUEUE + "?disableReplyTo=true&timeToLive="+getTimeToLive())
                .routeId("request.ws.14." + subscription.getSubscriptionType() + "." + subscription.getVendor())
        ;
    }
}
