package com.education.selection.config;

import com.education.common.constant.Constants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    @Bean
    public Queue selectionQueue() {
        return new Queue(Constants.QUEUE_SELECTION, true);
    }
    
    @Bean
    public DirectExchange selectionExchange() {
        return new DirectExchange(Constants.EXCHANGE_SELECTION, true, false);
    }
    
    @Bean
    public Binding selectionBinding() {
        return BindingBuilder.bind(selectionQueue())
                .to(selectionExchange())
                .with(Constants.ROUTING_KEY_SELECTION);
    }
    
    /**
     * 选课通知队列
     */
    @Bean
    public Queue selectionNotificationQueue() {
        return new Queue("selection.notification.queue", true);
    }
    
    /**
     * 选课通知绑定
     */
    @Bean
    public Binding selectionNotificationBinding() {
        return BindingBuilder.bind(selectionNotificationQueue())
                .to(selectionExchange())
                .with("selection.notification");
    }
    
    /**
     * 配置 RabbitTemplate 的消息转换器（用于发送消息）
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(new Jackson2JsonMessageConverter());
        return template;
    }
    
    /**
     * 配置消息监听器容器的消息转换器（用于接收消息）
     * 这是关键配置：@RabbitListener 需要使用这个转换器
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        return factory;
    }
}

