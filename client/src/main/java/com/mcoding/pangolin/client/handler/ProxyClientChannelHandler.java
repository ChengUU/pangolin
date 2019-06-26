package com.mcoding.pangolin.client.handler;

import com.mcoding.pangolin.Message;
import com.mcoding.pangolin.client.container.ClientContainer;
import com.mcoding.pangolin.client.entity.ProxyInfo;
import com.mcoding.pangolin.client.util.ChannelContextHolder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

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
    public void channelRead0(ChannelHandlerContext ctx, Message message) {
        switch (message.getType()) {
            case Message.TRANSFER:
                this.handleTansfer(ctx, message);
                break;
            default:
                break;
        }
    }

    private void handleTansfer(ChannelHandlerContext ctx, Message message) {
        Channel userChannel = ChannelContextHolder.getUserChannel();
        userChannel.writeAndFlush(Unpooled.wrappedBuffer(message.getData()));
    }

    private void handleConnectedMessage(ChannelHandlerContext ctx) {
        String realServerHost = proxyInfo.getRealServerHost();
        Integer realServerPort = proxyInfo.getRealServerPort();

        ChannelFuture futureChannel = this.realServerBootstrap.connect(realServerHost, realServerPort);
        futureChannel.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    log.info("EVENT=连接被代理服务器成功|HOST={}|PORT={}|CHANNEL={}", realServerHost, realServerPort, future.channel());
                } else {
                    log.info("event=连接被代理服务器失败");
                }
            }
        });

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.handleConnectedMessage(ctx);

        Message connectMsg = new Message();
        connectMsg.setUserId(proxyInfo.getUserId());
        connectMsg.setType(Message.CONNECTING);
        ctx.channel().writeAndFlush(connectMsg);

        ChannelContextHolder.addProxyChannel(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("EVENT=关闭已断开连接代理客户端|USER_CHANNEL={}|PROXY_CHANNEL={}",
                ChannelContextHolder.getUserChannel(), ChannelContextHolder.getProxyChannel());

        ChannelContextHolder.closeAll();
        this.clientContainer.channelInActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}