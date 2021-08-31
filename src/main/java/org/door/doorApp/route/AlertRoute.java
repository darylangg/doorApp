package org.door.doorApp.route;

import org.apache.camel.builder.RouteBuilder;
import org.door.common.protobuf.DoorDataProto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

//@Component
public class AlertRoute extends RouteBuilder {
    @Value("${door.exchange}")
    private String exchange;

    @Value("${door.web.queue.alert}")
    private String webAlertQueue;

    @Value("${door.app.queue.alert}")
    private String appAlertQueue;

    @Value("${door.routerKey.alert}")
    private String alertRoutingKey;

    @Value("${door.amqp.app.queue.properties}")
    private String queueProperties;

    @Override
    public void configure() throws Exception {
        from("rabbitmq:"+exchange+"?queue="+ webAlertQueue +"&autoDelete=false&declare=false&connectionFactory=#rabbitWebConnectionFactory")
            .routeId("Alert Route")
            .to("rabbitmq:"+exchange+"?routingKey="+ alertRoutingKey +"&queue="+ appAlertQueue +"&connectionFactory=#rabbitAppConnectionFactory");
    }
}
