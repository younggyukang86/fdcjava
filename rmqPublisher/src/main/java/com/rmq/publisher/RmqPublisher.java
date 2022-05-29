package com.rmq.publisher;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.concurrent.TimeoutException;

public  class RmqPublisher {
    private  final  static  String  QUEUE_NAME  =  "bbs.queue.java";
    private  final  static  String  RMQ_EXCHANGE  =  "bbs.exchange";
    private  final  static  String  RMQ_ROUTING_KEY  =  "bbs.central";
    private  final  static  String  RMQ_HOST  =  "127.0.0.1";
    private  final  static  int  RMQ_PORT  =  5672;
    private  final  static  String  RMQ_USERNAME  =  "rabbitmq";
    private  final  static  String  RMQ_PASSWORD  =  "rabbitmq";


    public  static  void  main(String[]  arg)  {
        System.out.println("RabbitMQ  Publisher  Start!");
        RmqPublisher  publisher  =  new RmqPublisher();
        publisher.initialStart();
    }

    protected  void  initialStart()  {
        try  {
            InputStream  in  =  System.in;
            InputStreamReader  reader  =  new InputStreamReader(in);
            BufferedReader  br  =  new BufferedReader(reader);

            while  (true)  {
                System.out.print("[Give the 'Message Send Sign'! Any word OK!]");
                String  inputMessage  =  br.readLine();

                if  (inputMessage.equals("out")) {
                    break;
                }

                AMQP.BasicProperties.Builder  propsBuilder  =  new  AMQP.BasicProperties.Builder();
                propsBuilder.appId("JAVA");


                ConnectionFactory  factory  =  new ConnectionFactory();
                factory.setHost(RMQ_HOST);
                factory.setPort(RMQ_PORT);
                factory.setUsername(RMQ_USERNAME);
                factory.setPassword(RMQ_PASSWORD);

                try  (Connection  connection  =  factory.newConnection();  Channel  channel  =  connection.createChannel())  {
                    String  message  =  ""; // "{'userId'  :  '"  +  inputMessage +  "',  'userName'  :  '"  +  inputMessage +  "'}";
                    String  queueName  =  channel.queueDeclare().getQueue();

                    channel.queueBind(queueName,  RMQ_EXCHANGE,  RMQ_ROUTING_KEY);
                    for (int i = 1; i <= 100000; i++ ) {
                        Date totime = new Date();
                        message = "[the number is " + i + "]";
                        System.out.println(message +" - " + totime);
                        channel.basicPublish(RMQ_EXCHANGE, RMQ_ROUTING_KEY, propsBuilder.build(), message.getBytes());
                    }

                }  catch  (TimeoutException  te)  {
                    te.printStackTrace();
                }  catch  (IOException  ioe)  {
                    ioe.printStackTrace();
                }

            }

        }  catch  (IOException  e)  {
           e.printStackTrace();
        }
    }
}