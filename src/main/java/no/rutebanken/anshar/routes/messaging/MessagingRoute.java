package no.rutebanken.anshar.routes.messaging;

import no.rutebanken.anshar.config.AnsharConfiguration;
import no.rutebanken.anshar.metrics.PrometheusMetricsService;
import no.rutebanken.anshar.routes.CamelRouteNames;
import no.rutebanken.anshar.routes.RestRouteBuilder;
import no.rutebanken.anshar.routes.dataformat.SiriDataFormatHelper;
import no.rutebanken.anshar.routes.siri.handlers.SiriHandler;
import no.rutebanken.anshar.subscription.SubscriptionManager;
import no.rutebanken.anshar.subscription.SubscriptionSetup;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.http.HttpMethods;
import org.apache.camel.support.builder.Namespaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;

import static no.rutebanken.anshar.routes.HttpParameter.PARAM_USE_ORIGINAL_ID;
import static no.rutebanken.anshar.routes.siri.Siri20RequestHandlerRoute.TRANSFORM_SOAP;
import static no.rutebanken.anshar.routes.siri.Siri20RequestHandlerRoute.TRANSFORM_VERSION;

@Service
public class MessagingRoute extends RestRouteBuilder {

    @Autowired
    AnsharConfiguration configuration;

    @Autowired
    private SiriHandler handler;

    @Autowired
    private SubscriptionManager subscriptionManager;

    @Autowired
    private PrometheusMetricsService metrics;

    @Override
    public void configure() throws Exception {

        String messageQueueCamelRoutePrefix = configuration.getMessageQueueCamelRoutePrefix();

        Namespaces ns = new Namespaces("siri", "http://www.siri.org.uk/siri")
                .add("xsd", "http://www.w3.org/2001/XMLSchema");

        String queueConsumerParameters = "?concurrentConsumers="+configuration.getConcurrentConsumers();


        final String pubsubQueueName = messageQueueCamelRoutePrefix + CamelRouteNames.TRANSFORM_QUEUE;

        if (messageQueueCamelRoutePrefix.contains("direct")) {
            queueConsumerParameters = "";
        }

        from("direct:enqueue.message")
                .setBody(body().convertToString())
                .to("direct:transform.siri")
                .process(p -> { // Remove lots of unecessary headers
                    p.getOut().setBody(p.getIn().getBody());
                    p.getOut().setHeader("subscriptionId", p.getIn().getHeader("subscriptionId"));
                    p.getOut().setHeader("breadcrumbId", p.getIn().getHeader("breadcrumbId"));
                })
//                .to("xslt-saxon:xsl/split.xsl").split().tokenizeXML("Siri").streaming()
                .choice()
                    .when().xpath("/siri:Siri/siri:DataReadyNotification", ns)
                    .to("direct:"+CamelRouteNames.FETCHED_DELIVERY_QUEUE)
                .endChoice()
                .otherwise()
                    .to("direct:compress.jaxb")
                  .to(pubsubQueueName)
                .end()
        ;

        from("direct:transform.siri")
                .choice()
                    .when(header(TRANSFORM_SOAP).isEqualTo(simple(TRANSFORM_SOAP)))
                    .log("Transforming SOAP")
                    .to("xslt-saxon:xsl/siri_soap_raw.xsl?allowStAX=false&resultHandlerFactory=#streamResultHandlerFactory") // Extract SOAP version and convert to raw SIRI
                .endChoice()
                .end()
                .choice()
                    .when(header(TRANSFORM_VERSION).isEqualTo(simple(TRANSFORM_VERSION)))
                    .log("Transforming version")
                    .to("xslt-saxon:xsl/siri_14_20.xsl?allowStAX=false&resultHandlerFactory=#streamResultHandlerFactory") // Convert from v1.4 to 2.0
                .endChoice()
                .end()
        ;

        from(pubsubQueueName + queueConsumerParameters)
                .to("direct:decompress.jaxb")
//                .to("direct:map.protobuf.to.jaxb")
                .log("Processing data from " + pubsubQueueName + ", size ${header.Content-Length}")
                .to("direct:" + CamelRouteNames.DEFAULT_PROCESSOR_QUEUE)
                .routeId("incoming.transform")
        ;
//
//        from("direct:" + CamelRouteNames.ROUTER_QUEUE)
//                .choice()
//                .when().xpath("/siri:Siri/siri:DataReadyNotification", ns)
//                    .to("direct:"+CamelRouteNames.FETCHED_DELIVERY_QUEUE)
//                .endChoice()
//                .otherwise()
//                    .to("direct:"+CamelRouteNames.DEFAULT_PROCESSOR_QUEUE)
//                .end()
//                .routeId("incoming.redirect")
//        ;

        from("direct:" + CamelRouteNames.DEFAULT_PROCESSOR_QUEUE)
                .process(p -> {

                    String subscriptionId = p.getIn().getHeader("subscriptionId", String.class);
                    String datasetId = null;

                    InputStream xml = p.getIn().getBody(InputStream.class);
                    String useOriginalId = p.getIn().getHeader(PARAM_USE_ORIGINAL_ID, String.class);
                    String clientTrackingName = p.getIn().getHeader(configuration.getTrackingHeaderName(), String.class);

                    handler.handleIncomingSiri(subscriptionId, xml, datasetId, SiriHandler.getIdMappingPolicy(useOriginalId), -1, clientTrackingName);

                })
                .routeId("incoming.processor.default")
        ;

        from("direct:" + CamelRouteNames.FETCHED_DELIVERY_QUEUE)
                .log("Processing fetched delivery")
                .process(p -> {
                    String routeName = null;

                    String subscriptionId = p.getIn().getHeader("subscriptionId", String.class);

                    SubscriptionSetup subscription = subscriptionManager.get(subscriptionId);
                    if (subscription != null) {
                        routeName = subscription.getServiceRequestRouteName();
                    }

                    p.getOut().setHeader("routename", routeName);

                })
                .choice()
                .when(header("routename").isNotNull())
                    .toD("direct:${header.routename}")
                .endChoice()
                .routeId("incoming.processor.fetched_delivery")
        ;

        if (configuration.getSiriVmPositionForwardingUrl() != null) {
            from("direct:forward.position.data")
                    .routeId("forward.position.data")
                    .bean(metrics, "countOutgoingData(${body}, VM_POSITION_FORWARDING)")
                    .marshal(SiriDataFormatHelper.getSiriJaxbDataformat())
                    .choice()
                        .when().xpath("/siri:Siri/siri:ServiceDelivery/siri:VehicleMonitoringDelivery", ns)
                            .removeHeaders("Camel*")
                            .setHeader(Exchange.HTTP_METHOD, constant(HttpMethods.POST))
                            .to(configuration.getSiriVmPositionForwardingUrl())
                        .endChoice()
                    .end()
            ;
        } else {
            from("direct:forward.position.data")
                    .log(LoggingLevel.INFO, "Ignoring position-update from ${header.subscriptionId}");
        }
    }
}
