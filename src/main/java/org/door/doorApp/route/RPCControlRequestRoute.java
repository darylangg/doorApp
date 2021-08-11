package org.door.doorApp.route;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.door.common.protobuf.DoorDataProto;
import org.door.doorApp.bean.HeartbeatBean;
import org.door.doorApp.bean.RPCRequestBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
public class RPCControlRequestRoute extends RouteBuilder {
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

    @Override
    public void configure() throws Exception {
        from ("rabbitmq:"+exchange+"?routingKey="+RPCRequestRoutingKey+"&queue="+appRPCRequestQueue+"&connectionFactory=#rabbitAppConnectionFactory")
            .choice().when(method(HeartbeatBean.getInstance(),"servicesConnected"))
            // process body
            .routeId("RPC Request Route")
            .bean(RPCRequestBean.getInstance(),"processRequest")
            // get door list if not provided
            .choice()
                .when(method(RPCRequestBean.getInstance(),"isGroupQuery"))
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
                .end()
            .bean(RPCRequestBean.getInstance(),"getLatestRequest")
            .log("Sending Request")
            .setHeader("CamelRabbitmqRoutingKey",simple(RPCRequestRoutingKey))
            .to("rabbitmq:"+exchange+"?queue="+ webRPCRequestQueue +"&declare=false&connectionFactory=#rabbitWebConnectionFactory")
            .bean(RPCRequestBean.getInstance(), "prepareDBPayload")
            .marshal().json()
            .process(e->{
                e.getMessage().removeHeaders("*");
                e.getMessage().setHeader(Exchange.HTTP_METHOD, HttpMethod.POST);
                e.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json");
            });
//            .to("http://"+DBAPIHost+":"+ DBVerticalAPIPort +"/api/vertical/v1/door/request/bulk");
    }
}
