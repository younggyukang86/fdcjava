package com.rmq.listener;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class RmqListener {
    private final static String QUEUE_NAME = "bbs.queue.java";
    private final static String RMQ_EXCHANGE = "bbs.exchange";
    private final static String RMQ_ROUTING_KEY = "bbs.central";
    private final static String RMQ_HOST = "127.0.0.1";
    private final static int RMQ_PORT = 5672;
    private final static String RMQ_USERNAME = "rabbitmq";
    private final static String RMQ_PASSWORD = "rabbitmq";

    public static void main(String[] args) {
        System.out.println("RabbitMQ  Listener  Start!");
        RmqListener listener = new RmqListener();
        listener.initialStart();
    }

    private void initialStart() {
        try {
            ConnectionFactory factory = getFactoryConnection();
            System.out.println("RabbitMQ Connection Success!");

            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(RMQ_EXCHANGE,"direct", true);
            System.out.println("RabbitMQ Exchange Declare Success!");
            channel.queueBind(QUEUE_NAME, RMQ_EXCHANGE, RMQ_ROUTING_KEY );
            System.out.println("RabbitMQ Queue Bind Success!");

            AtomicReference<Timestamp> totalStart = new AtomicReference<>(new Timestamp(System.currentTimeMillis()));
            AtomicLong start = new AtomicLong(System.currentTimeMillis());

            DeliverCallback callback = (consumerTag, delivery) -> {
                String msg = new String(delivery.getBody(), "UTF-8");

                if ("[the number is 2]".equals(msg)) {
                    totalStart.set(new Timestamp(System.currentTimeMillis()));
                    start.set(System.currentTimeMillis());
                } else if ("[the number is 100000]".equals(msg)) {
                    System.out.println("[TOTAL] START TIME : " + totalStart);
                    System.out.println("[TOTAL] END TIME : " + new Timestamp(System.currentTimeMillis()));
                    long end = System.currentTimeMillis();
                    System.out.println("수행시간 : " + new DecimalFormat("###.0").format((end - start.get()) / 1000.0) + " 초");
                }
            };

            channel.basicConsume(QUEUE_NAME, true, callback, consumerTag -> {});
            System.out.println("RabbitMQ Consume Create Success!");

        } catch (TimeoutException te)  {
            te.printStackTrace();
        } catch (IOException ioe)  {
            ioe.printStackTrace();
        }
    }

    private ConnectionFactory getFactoryConnection() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RMQ_HOST);
        factory.setPort(RMQ_PORT);
        factory.setUsername(RMQ_USERNAME);
        factory.setPassword(RMQ_PASSWORD);
        return  factory;
    }
}
