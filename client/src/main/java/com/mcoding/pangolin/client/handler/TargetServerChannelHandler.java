package com.mcoding.pangolin.client.handler;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.mcoding.pangolin.client.context.PangolinChannelContext;
import com.mcoding.pangolin.common.entity.AddressInfo;
import com.mcoding.pangolin.common.util.ChannelAddressUtils;
import com.mcoding.pangolin.common.constant.Constants;
import com.mcoding.pangolin.protocol.MessageType;
import com.mcoding.pangolin.protocol.PMessageOuterClass;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

/**
 * 目标服务通道处理器
 *
 * @author wzt on 2019/6/17.
 * @version 1.0
 */

@AllArgsConstructor
@Slf4j
@ChannelHandler.Sharable
public class TargetServerChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    public static final TargetServerChannelHandler INSTANCE = new TargetServerChannelHandler();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("EVENT=激活目标服务通道-{}", ctx.channel().toString());

        String sessionId = ctx.channel().attr(Constants.SESSION_ID).get();
        PangolinChannelContext.bindTargetServerChannel(sessionId, ctx.channel());

        AddressInfo targetServerChannelAddressInfo = ChannelAddressUtils.buildAddressInfo(ctx.channel());

        Channel intranetProxyChannel = PangolinChannelContext.getIntranetProxyChannel();
        AddressInfo intranetProxyChannelAddressInfo = ChannelAddressUtils.buildAddressInfo(intranetProxyChannel);

        List<AddressInfo> addressInfoList = Lists.newArrayList(targetServerChannelAddressInfo, intranetProxyChannelAddressInfo);

        PMessageOuterClass.PMessage chainTraceMsg = PMessageOuterClass.PMessage.newBuilder()
                .setType(MessageType.CHAIN_TRACE)
                .setData(ByteString.copyFrom(JSON.toJSONString(addressInfoList), Charset.defaultCharset()))
                .setSessionId(sessionId)
                .build();
        intranetProxyChannel.writeAndFlush(chainTraceMsg);

    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) {
        byte[] content = ByteBufUtil.getBytes(byteBuf);

        String sessionId = ctx.channel().attr(Constants.SESSION_ID).get();

        PMessageOuterClass.PMessage respMsg = PMessageOuterClass.PMessage.newBuilder()
                .setType(MessageType.TRANSFER)
                .setData(ByteString.copyFrom(content))
                .setSessionId(sessionId)
                .build();
        Channel proxyChannel = PangolinChannelContext.getIntranetProxyChannel();
        proxyChannel.writeAndFlush(respMsg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String sessionId = ctx.channel().attr(Constants.SESSION_ID).get();
        Channel userChannel = PangolinChannelContext.getTargetChannel(sessionId);
        if (Objects.isNull(userChannel)) {
            log.warn("EVENT=目标服务通道掉线，关闭用户通道|DESC=已关闭通道|SESSION_ID={}", sessionId);
        } else {
            log.warn("EVENT=目标服务通道掉线，关闭用户通道{}", PangolinChannelContext.getTargetChannel(sessionId));
        }

        PangolinChannelContext.unBindTargetServerChannel(sessionId);

        PMessageOuterClass.PMessage disconnectMsg = PMessageOuterClass.PMessage.newBuilder()
                .setSessionId(sessionId).setType(MessageType.DISCONNECT)
                .build();

        PangolinChannelContext.getIntranetProxyChannel().writeAndFlush(disconnectMsg);

        log.info("EVENT=关闭公网服务连接通道|SESSION_ID={}", sessionId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("EVENT=目标服务通道异常|CHANNEL={}|ERROR_DESC={}", ctx.channel(), cause.getMessage());
        ctx.close();
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        log.info("EVENT=目标服务管道可写状态变化" + ctx.channel().isWritable());
        super.channelWritabilityChanged(ctx);
    }
}