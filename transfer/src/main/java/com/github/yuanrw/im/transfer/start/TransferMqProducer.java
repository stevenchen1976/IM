package com.github.yuanrw.im.transfer.start;

import com.github.yuanrw.im.common.domain.constant.MqConstant;
import com.github.yuanrw.im.protobuf.constant.MsgTypeEnum;
import com.google.protobuf.Message;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Date: 2019-05-06
 * Time: 14:27
 *
 * @author yrw
 */
public class TransferMqProducer {
    private static Logger logger = LoggerFactory.getLogger(TransferMqProducer.class);

    private static Channel channel;

    static void startProducer(String host, int port, String username, String password)
        throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(MqConstant.EXCHANGE, BuiltinExchangeType.DIRECT, true, false, null);
        channel.queueDeclare(MqConstant.OFFLINE_QUEUE, true, false, false, null);
        channel.queueBind(MqConstant.OFFLINE_QUEUE, MqConstant.EXCHANGE, MqConstant.ROUTING_KEY);

        TransferMqProducer.channel = channel;
        logger.info("[transfer] producer start success");
    }

    public static void basicPublish(String exchange, String routingKey, AMQP.BasicProperties properties, Message message) throws IOException {
        int code = MsgTypeEnum.getByClass(message.getClass()).getCode();

        byte[] srcB = message.toByteArray();
        byte[] destB = new byte[srcB.length + 1];
        destB[0] = (byte) code;

        System.arraycopy(message.toByteArray(), 0, destB, 1, message.toByteArray().length);

        channel.basicPublish(exchange, routingKey, properties, destB);
    }

    public static Channel getChannel() {
        return channel;
    }
}
