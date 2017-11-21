package no.rutebanken.anshar.routes.siri;

import no.rutebanken.anshar.dataformat.SiriDataFormatHelper;
import no.rutebanken.anshar.routes.CamelConfiguration;
import no.rutebanken.anshar.routes.siri.handlers.SiriHandler;
import no.rutebanken.anshar.subscription.RequestType;
import no.rutebanken.anshar.subscription.SubscriptionManager;
import no.rutebanken.anshar.subscription.models.Subscription;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.http.common.HttpMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.ConnectException;
import java.util.Map;

import static no.rutebanken.anshar.routes.siri.SiriRequestFactory.getCamelUrl;
import static no.rutebanken.anshar.subscription.SubscriptionHelper.*;

public class Siri20ToSiriRS20Subscription extends SiriSubscriptionRouteBuilder {

    private Logger logger = LoggerFactory.getLogger(Siri20ToSiriRS20Subscription.class);

    private SiriHandler handler;

    public Siri20ToSiriRS20Subscription(CamelConfiguration config, SiriHandler handler, Subscription subscription, SubscriptionManager subscriptionManager) {
        super(config, subscriptionManager);
        this.handler = handler;
        this.subscription = subscription;
    }

    @Override
    public void configure() throws Exception {

        Map<RequestType, String> urlMap = subscription.getUrlMap();
        SiriRequestFactory helper = new SiriRequestFactory(subscription);

        //Start subscription
        from("direct:" + getStartSubscriptionRouteName(subscription))
                .log("Starting subscription " + subscription.toString())
                .bean(helper, "createSiriSubscriptionRequest", false)
                .marshal(SiriDataFormatHelper.getSiriJaxbDataformat())
                .setExchangePattern(ExchangePattern.InOut) // Make sure we wait for a response
                .setHeader("operatorNamespace", constant(subscription.getOperatorNamespace())) // Need to make SOAP request with endpoint specific element namespace
                .removeHeaders("CamelHttp*") // Remove any incoming HTTP headers as they interfere with the outgoing definition
                .setHeader(Exchange.CONTENT_TYPE, constant(subscription.getContentType())) // Necessary when talking to Microsoft web services
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .to("log:sent request:" + getClass().getSimpleName() + "?showAll=true&multiline=true")
                .doTry()
                    .to(getCamelUrl(urlMap.get(RequestType.SUBSCRIBE), getTimeout()))
                    .to("log:received response:" + getClass().getSimpleName() + "?showAll=true&multiline=true")
                    .process(p -> {

                        String responseCode = p.getIn().getHeader("CamelHttpResponseCode", String.class);
                        InputStream body = p.getIn().getBody(InputStream.class);
                        if (body != null && body.available() > 0) {
                            handler.handleIncomingSiri(subscription.getSubscriptionId(), body);
                        } else if ("200".equals(responseCode)) {
                            logger.info("SubscriptionResponse OK - Async response performs actual registration");
                            subscriptionManager.activatePendingSubscription(subscription.getSubscriptionId());
                        } else {
                            hasBeenStarted = false;
                        }

                    })
                .doCatch(ConnectException.class)
                    .log("Caught ConnectException - subscription not started - will try again: "+ subscription.toString())
                    .process(p -> {
                        p.getOut().setBody(null);
                    })
                .endDoTry()
                .routeId("start.rs.20.subscription."+subscription.getVendor())
        ;

        //Check status-request checks the server status - NOT the subscription
        from("direct:" + getCheckStatusRouteName(subscription))
        		.bean(helper, "createSiriCheckStatusRequest", false)
        		.marshal(SiriDataFormatHelper.getSiriJaxbDataformat())
                .removeHeaders("CamelHttp*") // Remove any incoming HTTP headers as they interfere with the outgoing definition
                .setHeader(Exchange.CONTENT_TYPE, constant(subscription.getContentType())) // Necessary when talking to Microsoft web services
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .to(getCamelUrl(urlMap.get(RequestType.CHECK_STATUS), getTimeout()))
                .process(p -> {

                    String responseCode = p.getIn().getHeader("CamelHttpResponseCode", String.class);
                    if ("200" .equals(responseCode)) {
                        logger.trace("CheckStatus OK - Remote service is up [{}]", buildUrl(subscription));
                        InputStream body = p.getIn().getBody(InputStream.class);
                        if (body != null && body.available() > 0) {
                            handler.handleIncomingSiri(subscription.getSubscriptionId(), body);
                        }
                    } else {
                        logger.info("CheckStatus NOT OK - Remote service is down [{}]", buildUrl(subscription));
                    }

                })
                .routeId("check.status.rs.20.subscription."+subscription.getVendor())
        ;

        //Cancel subscription
        from("direct:" + getCancelSubscriptionRouteName(subscription))
                .log("Cancelling subscription " + subscription.toString())
                .bean(helper, "createSiriTerminateSubscriptionRequest", false)
                .marshal(SiriDataFormatHelper.getSiriJaxbDataformat())
                .setExchangePattern(ExchangePattern.InOut) // Make sure we wait for a response
                .setProperty(Exchange.LOG_DEBUG_BODY_STREAMS, constant("true"))
                .removeHeaders("CamelHttp*") // Remove any incoming HTTP headers as they interfere with the outgoing definition
                .setHeader(Exchange.CONTENT_TYPE, constant(subscription.getContentType())) // Necessary when talking to Microsoft web services
                .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                .to(getCamelUrl(urlMap.get(RequestType.DELETE_SUBSCRIPTION), getTimeout()))
                .to("log:received response:" + getClass().getSimpleName() + "?showAll=true&multiline=true")
                .process(p -> {
                    InputStream body = p.getIn().getBody(InputStream.class);
                    if (body != null && body.available() >0) {
                        handler.handleIncomingSiri(subscription.getSubscriptionId(), body);
                    }
                })
                .routeId("cancel.rs.20.subscription."+subscription.getVendor())
        ;

        initTriggerRoutes();
    }

}
