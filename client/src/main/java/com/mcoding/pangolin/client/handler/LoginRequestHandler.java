package com.mcoding.pangolin.client.handler;

import com.google.protobuf.ByteString;
import com.mcoding.pangolin.client.context.PangolinChannelContext;
import com.mcoding.pangolin.common.constant.Constants;
import com.mcoding.pangolin.protocol.MessageType;
import com.mcoding.pangolin.protocol.PMessageOuterClass;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 心跳发送处理器
 *
 * @author wzt on 2019/10/16.
 * @version 1.0
 */
@Slf4j
@AllArgsConstructor
public class LoginRequestHandler extends SimpleChannelInboundHandler<PMessageOuterClass.PMessage> {

    private String privateKey;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PMessageOuterClass.PMessage message) {

        ByteString data = message.getData();
        if (Constants.LOGIN_SUCCESS.equalsIgnoreCase(data.toStringUtf8())) {
            ctx.channel().attr(Constants.SESSION_ID).set(message.getSessionId());
            ctx.channel().attr(Constants.PRIVATE_KEY).set(message.getPrivateKey());
            PangolinChannelContext.bindIntranetProxyChannel(ctx.channel());
            log.info("EVENT=登陆成功");

            ctx.pipeline().remove(this);

        } else {
            log.error("EVENT=登陆异常|DESC={}", data.toStringUtf8());
            System.exit(0);
        }


    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 发送认证私钥
        PMessageOuterClass.PMessage connectMsg = PMessageOuterClass.PMessage.newBuilder()
                .setPrivateKey(privateKey)
                .setType(MessageType.LOGIN)
                .build();
        ctx.channel().writeAndFlush(connectMsg);
    }
}
