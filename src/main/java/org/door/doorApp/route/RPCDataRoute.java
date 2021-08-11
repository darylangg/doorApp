package org.door.doorApp.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.rabbitmq.RabbitMQConstants;
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

    @Override
    public void configure() throws Exception {
        from("rabbitmq:"+exchange+"?routingKey="+RPCDataRoutingKey+"&queue="+ RPCDataQueue +"&autoDelete=true&connectionFactory=#rabbitAppConnectionFactory")
            .routeId("RPC Data Route")
            .choice().when(method(DoorFilterBean.getInstance(), "hasData"))
            .process(e->{
                Object routingKey = e.getMessage().getHeader(RabbitMQConstants.REPLY_TO);
                Object correlationID = e.getMessage().getHeader(RabbitMQConstants.CORRELATIONID);
                e.getMessage().removeHeaders("*");
                e.getMessage().setHeader(RabbitMQConstants.CORRELATIONID, correlationID);
                e.getMessage().setHeader(RabbitMQConstants.ROUTING_KEY, routingKey);
            })
            .bean(DoorFilterBean.getInstance(),"getLatestData")
            .to("rabbitmq:"+exchange+"?declare=false&connectionFactory=#rabbitAppConnectionFactory");
    }
}
