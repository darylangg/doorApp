package org.door.doorApp.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.rabbitmq.RabbitMQConstants;
import org.door.common.utilities.QueuePropertiesReader;
import org.door.doorApp.bean.DoorFilterBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RPCDataRoute extends RouteBuilder {
    @Value("${door.exchange}")
    private String exchange;

    @Value("${door.routerKey.RPCDataRequest}")
    private String RPCDataRoutingKey;

    @Value("${door.app.queue.RPCDataRequest}")
    private String RPCDataQueue;

    private String queueProperties;

    @Override
    public void configure() throws Exception {
        queueProperties = QueuePropertiesReader.getInstance().getProperties();
        from("rabbitmq:"+exchange+"?routingKey="+RPCDataRoutingKey+"&queue="+ RPCDataQueue +queueProperties)
            .routeId("RPC Data Route")
                .log("RPC Data request received")
                .log("${headers}")
            .choice().when(method(DoorFilterBean.getInstance(), "hasData"))
            .removeHeaders(RabbitMQConstants.ROUTING_KEY)
            .log("${headers}")
            .bean(DoorFilterBean.getInstance(),"getLatestData")
            .to("rabbitmq:"+exchange+"?skipQueueDeclare=true&connectionFactory=#rabbitAppConnectionFactory&exchangePattern=InOnly");
    }
}
