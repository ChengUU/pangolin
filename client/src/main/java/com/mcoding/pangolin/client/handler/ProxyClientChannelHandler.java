package com.mcoding.pangolin.client.handler;

import com.alibaba.fastjson.JSON;
import com.mcoding.pangolin.Message;
import com.mcoding.pangolin.client.container.ClientContainer;
import com.mcoding.pangolin.client.entity.ProxyInfo;
import com.mcoding.pangolin.client.util.ChannelContextHolder;
import com.mcoding.pangolin.common.Constants;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author wzt on 2019/6/17.
 * @version 1.0
 */
@Slf4j
public class ProxyClientChannelHandler extends SimpleChannelInboundHandler<Message> {

    private ProxyInfo proxyInfo;
    private Bootstrap realServerBootstrap;
    private ClientContainer clientContainer;

    public ProxyClientChannelHandler(ProxyInfo proxyInfo, Bootstrap realServerBootstrap, ClientContainer clientContainer) {
        this.proxyInfo = proxyInfo;
        this.realServerBootstrap = realServerBootstrap;
        this.clientContainer = clientContainer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 发送认证私钥
        Message connectMsg = new Message();
        connectMsg.setPrivateKey(proxyInfo.getPrivateKey());
        connectMsg.setType(Message.AUTH);
        ctx.channel().writeAndFlush(connectMsg);

        ChannelContextHolder.addProxyChannel(ctx.channel());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message message) {
        switch (message.getType()) {
            case Message.CONNECT:
                this.handleConnectedMessage(ctx, message);
                break;
            case Message.TRANSFER:
                this.handleTransfer(ctx, message);
                break;
            default:
                break;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("EVENT=用户通道掉线，关闭所有通道{}", ChannelContextHolder.getAllChannelList());

        ChannelContextHolder.closeAll();
        this.clientContainer.channelInActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void handleTransfer(ChannelHandlerContext ctx, Message message) {
        System.out.println("已建立通道类型： " + ChannelContextHolder.getAllChannelList());
        Channel userChannel = ChannelContextHolder.getUserChannel(message.getSessionId());
        if (Objects.isNull(userChannel)) {
            try{
                TimeUnit.SECONDS.sleep(3);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        System.out.println(JSON.toJSONString(message));
        userChannel.writeAndFlush(Unpooled.wrappedBuffer(message.getData()));
    }

    private void handleConnectedMessage(ChannelHandlerContext ctx, Message message) {
        String sessionId = message.getSessionId();
        Channel userChannel = ChannelContextHolder.getUserChannel(sessionId);
        if (Objects.nonNull(userChannel)) {
            log.info("EVENT=连接被代理服务|DESC=通道已连接，不需要重新连接");
            return;
        }

        String realServerHost = proxyInfo.getRealServerHost();
        Integer realServerPort = proxyInfo.getRealServerPort();

        ChannelFuture futureChannel = this.realServerBootstrap
                .connect(realServerHost, realServerPort);
        futureChannel.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    log.info("EVENT=连接被代理服务器成功|HOST={}|PORT={}|CHANNEL={}", realServerHost, realServerPort, future.channel());
                    future.channel().attr(Constants.SESSION_ID).set(sessionId);
                    ChannelContextHolder.addUserChannel(sessionId, futureChannel.channel());

                    Message confirmConnectMsg = new Message();
                    confirmConnectMsg.setSessionId(sessionId);
                    confirmConnectMsg.setType(Message.CONNECT);

                    ctx.channel().writeAndFlush(confirmConnectMsg);
                } else {
                    log.info("event=连接被代理服务器失败");
                }
            }
        });
    }

}