package org.door.doorApp.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.rabbitmq.RabbitMQConstants;
import org.door.doorApp.bean.HeartbeatBean;
import org.door.doorApp.bean.RPCRequestBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RPCControlRoute extends RouteBuilder {
    @Value("${door.exchange}")
    private String exchange;

    @Value("${door.routerKey.RPCControlRequest}")
    private String RPCRequestRoutingKey;

    @Value("${door.web.queue.RPCControlRequest}")
    private String webRPCRequestQueue;

    @Value("${door.app.queue.RPCControlRequest}")
    private String appRPCRequestQueue;

    @Value("${door.database.api.host}")
    private String DBAPIHost;

    @Value("${door.database.api.general.port}")
    private String DBGeneralAPIPort;

    @Value("${door.database.api.vertical.port}")
    private String DBVerticalAPIPort;

    @Value("${door.routerKey.RPCControlReply}")
    private String RPCReplyRoutingKey;

    @Value("${door.app.queue.RPCControlReply}")
    private String appRPCReplyQueue;

    @Value("${door.web.queue.RPCControlReply}")
    private String webRPCReplyQueue;

    @Value("${door.amqp.app.queue.properties}")
    private String appQueueProperties;

    @Value("${door.amqp.web.queue.properties}")
    private String webQueueProperties;

    @Override
    public void configure() throws Exception {
        from ("rabbitmq:"+exchange+"?routingKey="+RPCRequestRoutingKey+"&queue="+appRPCRequestQueue+ appQueueProperties)
            .choice().when(method(HeartbeatBean.getInstance(),"servicesConnected"))
                .log("${headers}")
            // process body
            .routeId("RPC Request Route")
            .bean(RPCRequestBean.getInstance(),"processRequest")
                .process(e->{
                    RPCRequestBean.getInstance().setCorrelationID((String) e.getMessage().getHeader(RabbitMQConstants.CORRELATIONID));
                    RPCRequestBean.getInstance().setReplyTo((String) e.getMessage().getHeader(RabbitMQConstants.REPLY_TO));
                })
            // get door list if not provided
            .choice().when(method(RPCRequestBean.getInstance(),"isGroupQuery"))
                .process(e->{
                    // prepare query
                    e.getMessage().setHeader(Exchange.HTTP_QUERY,"group_name="+RPCRequestBean.getInstance().getGroupName());
                })
                .log("Getting door list from db...")
                .to("http://"+DBAPIHost+":"+ DBGeneralAPIPort +"/api/general/v1/device/group")
                .unmarshal().json()
                .bean(RPCRequestBean.getInstance(),"JSONtoRequestProto")
            .otherwise()
                .log("Door list acquired from request")
            .endChoice()
            .end()
            .onCompletion().parallelProcessing()
                .to("direct:sendRequest")
                .delay(1000)
                .choice().when(method(RPCRequestBean.getInstance(), "requestCompleted"))
                    .log("Sending Reply...")
                    .process(e->{
                        e.getMessage().setHeader(RabbitMQConstants.REPLY_TO,RPCRequestBean.getInstance().getReplyTo());
                        e.getMessage().setHeader(RabbitMQConstants.CORRELATIONID,RPCRequestBean.getInstance().getCorrelationID());
                    })
                    .bean(RPCRequestBean.getInstance(), "getLatestReply")
                    //&exchangePattern=InOnly
                    .to("rabbitmq:"+exchange+"?skipQueueDeclare=true&connectionFactory=#rabbitAppConnectionFactory&autoDelete=false");

        from("direct:sendRequest")
            .routeId("Control Request Queue Route")
            .bean(RPCRequestBean.getInstance(),"getLatestRequest")
            .removeHeaders("*")
            .log("Sending Request...")
            .to("rabbitmq:"+exchange+"?routingKey="+RPCRequestRoutingKey+"&queue="+ webRPCRequestQueue +webQueueProperties);

        from("rabbitmq:"+exchange+"?routingKey="+RPCReplyRoutingKey+"&queue="+ webRPCReplyQueue +webQueueProperties)
            .bean(RPCRequestBean.getInstance(),"processReply")
            .log("Reply received");

        //    .bean(RPCRequestBean.getInstance(), "prepareDBPayload")
//            .marshal().json()
//            .process(e->{
//                e.getMessage().removeHeaders("*");
//                e.getMessage().setHeader(Exchange.HTTP_METHOD, HttpMethod.POST);
//                e.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json");
//            })
//            .to("http://"+DBAPIHost+":"+ DBVerticalAPIPort +"/api/vertical/v1/door/request/bulk");
    }
}
