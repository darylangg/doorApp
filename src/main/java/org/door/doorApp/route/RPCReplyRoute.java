package org.door.doorApp.route;

import org.apache.camel.builder.RouteBuilder;
import org.door.common.protobuf.DoorDataProto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RPCReplyRoute extends RouteBuilder {
    @Value("${door.exchange}")
    private String exchange;

    @Value("${door.routerKey.RPCControlReply}")
    private String RPCReplyRoutingKey;

    @Value("${door.web.queue.RPCControlReply}")
    private String webRPCReplyQueue;

    @Value("${door.app.queue.RPCControlReply}")
    private String appRPCReplyQueue;

    @Override
    public void configure() throws Exception {
        from("rabbitmq:"+exchange+"?queue="+ webRPCReplyQueue +"&autoDelete=false&declare=false&connectionFactory=#rabbitWebConnectionFactory")
            .routeId("RPC Reply Route")
            .process(e->{
                e.getMessage().removeHeaders("*");
            })
            .log("Reply received")
            .to("rabbitmq:"+exchange+"?routingKey="+ RPCReplyRoutingKey +"&queue="+ appRPCReplyQueue +"&connectionFactory=#rabbitAppConnectionFactory");
    }
}
