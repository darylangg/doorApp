package org.door.doorApp.route;

import org.apache.camel.builder.RouteBuilder;
import org.door.common.protobuf.DoorDataProto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AlertRoute extends RouteBuilder {
    @Value("${door.exchange}")
    private String exchange;

    @Value("${door.queue.alert}")
    private String alertQueue;

    @Value("${door.routerKey.alert}")
    private String alertRoutingKey;

    @Override
    public void configure() throws Exception {
        from("rabbitmq:"+exchange+"?queue="+ alertQueue +"&autoDelete=false&declare=false&connectionFactory=#rabbitWebConnectionFactory")
            .routeId("Alert Route")
            .to("rabbitmq:"+exchange+"?routingKey="+ alertRoutingKey +"&queue="+ alertQueue +"&connectionFactory=#rabbitAppConnectionFactory");
    }
}
