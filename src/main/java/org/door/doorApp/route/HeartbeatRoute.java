package org.door.doorApp.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.door.common.protobuf.HeartbeatProto;
import org.door.doorApp.bean.HeartbeatBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HeartbeatRoute extends RouteBuilder {
    @Value("${door.exchange}")
    private String exchange;

    @Value("${door.web.queue.heartbeat}")
    private String webHeartbeatQueue;

    @Value("${door.app.queue.heartbeat}")
    private String appHeartbeatQueue;

    @Value("${door.routerKey.heartbeat}")
    private String heartbeatRoutingKey;

    @Value("${door.database.api.host}")
    private String DBAPIHost;

    @Value("${door.database.api.general.port}")
    private String DBGeneralAPIPort;

    @Value("${door.database.api.vertical.port}")
    private String DBVerticalAPIPort;

    @Override
    public void configure() throws Exception {
        // web heartbeat
        from("rabbitmq:"+exchange+"?queue="+ webHeartbeatQueue +"&autoDelete=false&declare=false&connectionFactory=#rabbitWebConnectionFactory")
            .routeId("Web Heartbeat Route")
            .process(e->{
                e.getMessage().removeHeaders("*");
                byte[] data = e.getMessage().getBody(byte[].class);
                HeartbeatProto.Heartbeat incHeartbeat = HeartbeatProto.Heartbeat.parseFrom(data);
                HeartbeatBean.getInstance().processHeartbeatProto(incHeartbeat);
            });

        // Hapi heartbeat
        from("timer://hapi_heartbeat?fixedRate=true&delay=0&period=10000")
            .routeId("Hapi Heartbeat Route")
            .doTry()
                .to("http://"+DBAPIHost+":"+ DBGeneralAPIPort)
                .process(e->{
                    int responseCode = (int) e.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE);
                    if (responseCode == 200){
                        HeartbeatBean.getInstance().processHeartbeat("HAPI_GENERAL_API");
                    }
                    e.getMessage().reset();
                })
            .end()
            .doTry()
                .to("http://"+DBAPIHost+":"+ DBVerticalAPIPort)
                .process(e->{
                    int responseCode = (int) e.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE);
                    if (responseCode == 200){
                        HeartbeatBean.getInstance().processHeartbeat("HAPI_VERTICAL_API");
                    }
                })
            .end();

        // check all heartbeats
        from("timer://heartbeat?fixedRate=true&delay=0&period=10000")
            .routeId("Heartbeat Route")
            .bean(HeartbeatBean.getInstance(), "packHeartbeatProto")
            .to("rabbitmq:"+exchange+"?routingKey="+ heartbeatRoutingKey +"&queue="+ appHeartbeatQueue +"&connectionFactory=#rabbitAppConnectionFactory");
    }
}
