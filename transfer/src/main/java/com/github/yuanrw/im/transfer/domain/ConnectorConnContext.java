package com.github.yuanrw.im.transfer.domain;

import com.github.yuanrw.im.common.domain.conn.ConnectorConn;
import com.github.yuanrw.im.common.domain.conn.MemoryConnContext;
import com.github.yuanrw.im.common.util.IdWorker;
import com.github.yuanrw.im.protobuf.generate.Internal;
import com.github.yuanrw.im.user.status.factory.UserStatusServiceFactory;
import com.github.yuanrw.im.user.status.service.UserStatusService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.channel.ChannelHandlerContext;

import java.util.Properties;

import static com.github.yuanrw.im.transfer.start.TransferStarter.TRANSFER_CONFIG;

/**
 * 存储transfer和connector的连接
 * 以及用户和connector的关系
 * Date: 2019-04-12
 * Time: 18:22
 *
 * @author yrw
 */
@Singleton
public class ConnectorConnContext extends MemoryConnContext<ConnectorConn> {

    private UserStatusService userStatusService;

    @Inject
    public ConnectorConnContext(UserStatusServiceFactory userStatusServiceFactory) {
        Properties properties = new Properties();
        properties.put("host", TRANSFER_CONFIG.getRedisHost());
        properties.put("port", TRANSFER_CONFIG.getRedisPort());
        properties.put("password", TRANSFER_CONFIG.getRedisPassword());
        this.userStatusService = userStatusServiceFactory.createService(properties);
    }

    public void online(ChannelHandlerContext ctx, String userId) {
        String oldConnectorId = userStatusService.online(getConn(ctx).getNetId().toString(), userId);
        if (oldConnectorId != null) {
            //if the user is online, make him offline
            ConnectorConn conn = getConn(oldConnectorId);
            if (conn != null) {
                Internal.InternalMsg forceOffline = Internal.InternalMsg.newBuilder()
                    .setVersion(1)
                    .setId(IdWorker.genId())
                    .setCreateTime(System.currentTimeMillis())
                    .setFrom(Internal.InternalMsg.Module.TRANSFER)
                    .setDest(Internal.InternalMsg.Module.CONNECTOR)
                    .setMsgType(Internal.InternalMsg.MsgType.FORCE_OFFLINE)
                    .setMsgBody(userId + "")
                    .build();

                conn.getCtx().writeAndFlush(forceOffline);
            }
        }
    }

    public void offline(String userId) {
        userStatusService.offline(userId);
    }

    public ConnectorConn getConnByUserId(String userId) {
        String connectorId = userStatusService.getConnectorId(userId);
        if (connectorId != null) {
            ConnectorConn conn = getConn(connectorId);
            if (conn != null) {
                return conn;
            } else {
                //connectorId已过时，而用户还没再次上线
                userStatusService.offline(userId);
            }
        }
        return null;
    }
}
