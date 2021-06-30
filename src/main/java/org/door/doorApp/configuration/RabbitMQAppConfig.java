package org.door.doorApp.configuration;

import com.rabbitmq.client.ConnectionFactory;
import org.door.common.utilities.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLSocketFactory;

@Configuration
public class RabbitMQAppConfig {
    @Value("${door.amqp.app.router.user}")
    public String username;

    @Value("${door.amqp.app.router.password}")
    public String password;

    @Value("${door.amqp.app.router.host}")
    public String host;

    @Value("${door.amqp.app.router.port}")
    public Integer port;

    @Value("${door.amqp.app.router.useSSL}")
    protected boolean appUseSSL;

    @Value("${door.amqp.app.router.ssl_ca}")
    protected String caPathWeb;

    @Value("${door.amqp.app.router.ssl_cert}")
    protected String certPathWeb;

    @Value("${door.amqp.app.router.ssl_key}")
    protected String keyPathWeb;

    @Value("${door.amqp.app.router.ssl_keyPassword}")
    protected String keyPWWeb;


    @Bean
    public ConnectionFactory rabbitAppConnectionFactory(){
        Logger logger = LoggerFactory.getLogger(RabbitMQAppConfig.class);
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);

        if (appUseSSL) {
            try {
                SSLSocketFactory socketFactory = null;
                socketFactory = Util.getSocketFactory(caPathWeb, certPathWeb, keyPathWeb, keyPWWeb);
                connectionFactory.setSocketFactory(socketFactory);

            } catch (Exception e) {
                logger.info(e.toString());
                logger.info("unable to setup rabbit mq client with ssl connection");
            }
        }

        return connectionFactory;
    }
}
