package com.mcoding.pangolin.server.handler;

import com.mcoding.pangolin.common.codec.HeartBeatPacket;
import com.mcoding.pangolin.common.entity.AddressInfo;
import com.mcoding.pangolin.common.util.ChannelAddressUtils;
import com.mcoding.pangolin.common.util.DateTimeKit;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳处理器
 *
 * @author wzt on 2019/10/31.
 * @version 1.0
 */
@Slf4j
@ChannelHandler.Sharable
public class IntranetHeartBeatResponseHandler extends SimpleChannelInboundHandler<HeartBeatPacket> {

    public static final IntranetHeartBeatResponseHandler INSTANCE =new IntranetHeartBeatResponseHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HeartBeatPacket packet) {
        Channel channel=ctx.channel();
        AddressInfo addressInfo= ChannelAddressUtils.buildAddressInfo(channel);
        log.info("{}:{}-{}:{}|EVENT=收到心跳包|MSG={}|timestamp={}", addressInfo.getRemoteIp(),
                addressInfo.getRemotePort(),
                addressInfo.getLocalIp(),
                addressInfo.getLocalPort(),
                new String(packet.getData()),
                DateTimeKit.date());
    }

}
