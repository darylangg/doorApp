package org.door.doorApp.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.rabbitmq.RabbitMQConstants;
import org.door.common.protobuf.DoorDataProto;
import org.door.common.utilities.QueuePropertiesReader;
import org.door.doorApp.bean.HeartbeatBean;
import org.door.doorApp.bean.RPCRequestBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
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

    private String queueProperties;

    @Override
    public void configure() throws Exception {
        queueProperties = QueuePropertiesReader.getInstance().getProperties();

        from ("rabbitmq:"+exchange+"?routingKey="+RPCRequestRoutingKey+"&queue="+appRPCRequestQueue+queueProperties)
            .choice().when(method(HeartbeatBean.getInstance(),"servicesConnected"))
                .log("${headers}")
            // process body
            .routeId("RPC Request Route")
            .bean(RPCRequestBean.getInstance(),"processRequest")
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
                .log("${headers}")
            .onCompletion().parallelProcessing()
                .to("direct:sendRequest")
                .delay(1000)
                .choice().when(method(RPCRequestBean.getInstance(), "requestCompleted"))
                    .log("Sending Reply...")
                    .bean(RPCRequestBean.getInstance(), "getLatestReply")
                    .to("rabbitmq:"+exchange+"?skipQueueDeclare=true&connectionFactory=#rabbitAppConnectionFactory&exchangePattern=InOnly");

        from("direct:sendRequest")
            .routeId("Control Request Queue Route")
            .bean(RPCRequestBean.getInstance(),"getLatestRequest")
            .removeHeaders("*")
            .log("Sending Request...")
            .to("rabbitmq:"+exchange+"?routingKey="+RPCRequestRoutingKey+"&queue="+ webRPCRequestQueue +"&declare=false&connectionFactory=#rabbitWebConnectionFactory");

        from("rabbitmq:"+exchange+"?routingKey="+RPCReplyRoutingKey+"&queue="+ webRPCReplyQueue +"&autoDelete=false&declare=false&connectionFactory=#rabbitWebConnectionFactory")
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
