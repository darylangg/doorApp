package org.door.doorApp.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.rabbitmq.RabbitMQConstants;
import org.door.common.protobuf.DoorDataProto;
import org.door.doorApp.bean.DoorFilterBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StatusRoute extends RouteBuilder {
    @Value("${door.exchange}")
    private String exchange;

    @Value("${door.routerKey.status}")
    private String statusRouterKey;

    @Value("${door.queue.status}")
    private String statusWebQueue;

    @Value("${door.routerKey.csv}")
    private String csvRoutingKey;

    @Value("${door.queue.csv}")
    private String csvQueue;

    @Override
    public void configure() throws Exception {
        from("rabbitmq:"+exchange+"?routingKey="+statusRouterKey+"&queue="+ statusWebQueue +"&autoDelete=false&declare=false&connectionFactory=#rabbitWebConnectionFactory")
            .routeId("Status Route")
            .process(e->{
                e.getMessage().removeHeader(RabbitMQConstants.ROUTING_KEY);
                byte[] data = e.getMessage().getBody(byte[].class);
                DoorFilterBean.getInstance().checkIfNew(data);
            })
            .choice()
            .when(method(DoorFilterBean.getInstance(), "hasUpdates"))
                .bean(DoorFilterBean.getInstance(), "getLatestUpdate")
                .log("Sending new data")
                .to("rabbitmq:"+exchange+"?routingKey="+ csvRoutingKey +"&queue="+ csvQueue +"&connectionFactory=#rabbitAppConnectionFactory")
            .otherwise()
                .log("No new data");
    }
}
