package com.mcoding.pangolin.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 服务空闲检测器
 *
 * @author wzt on 2019/10/16.
 * @version 1.0
 */
@Slf4j
public class ServerIdleStateHandler extends IdleStateHandler {

    /** 通道状态检测-读：120秒 */
    private static final long READ_IDLE_TIME = 120;

    public ServerIdleStateHandler() {
        super(READ_IDLE_TIME, 0, 0, TimeUnit.SECONDS);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
        if(IdleStateEvent.READER_IDLE_STATE_EVENT==evt) {
            log.error("EVENT=连接空闲检测|DESC=连接超过{}秒没有读取到数据, 关闭连接",READ_IDLE_TIME);
            ctx.channel().close();
        }
    }

}
