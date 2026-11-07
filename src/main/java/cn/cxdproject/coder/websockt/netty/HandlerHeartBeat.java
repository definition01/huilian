package cn.cxdproject.coder.websockt.netty;

import cn.cxdproject.coder.websockt.ChannelContextUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳检测处理器
 * 检测客户端是否在线，超时则断开连接
 *
 * @author heathcetide
 */
@Slf4j
public class HandlerHeartBeat extends ChannelDuplexHandler {

    /**
     * 最大心跳丢失次数
     */
    private final int maxHeartbeatMiss;

    /**
     * 心跳丢失计数器的 AttributeKey
     */
    private static final String HEARTBEAT_MISS_COUNT = "heartbeat_miss_count";

    public HandlerHeartBeat(int maxHeartbeatMiss) {
        this.maxHeartbeatMiss = maxHeartbeatMiss;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;

            if (event.state() == IdleState.READER_IDLE) {
                // 读空闲：客户端长时间没有发送消息
                handleReaderIdle(ctx);
            } else if (event.state() == IdleState.WRITER_IDLE) {
                // 写空闲：服务器长时间没有发送消息，主动发送心跳
                handleWriterIdle(ctx);
            } else if (event.state() == IdleState.ALL_IDLE) {
                // 全部空闲
                log.debug("全部空闲 - Channel: {}", ctx.channel().id());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 处理读空闲（客户端长时间没有发送消息）
     */
    private void handleReaderIdle(ChannelHandlerContext ctx) {
        // 获取用户ID
        String userId = getUserId(ctx);

        // 获取心跳丢失计数
        Integer missCount = getMissCount(ctx);
        missCount++;
        setMissCount(ctx, missCount);

        log.warn("检测到读空闲 - 用户ID: {}, Channel: {}, 心跳丢失次数: {}/{}",
                userId, ctx.channel().id(), missCount, maxHeartbeatMiss);

        if (missCount >= maxHeartbeatMiss) {
            // 超过最大丢失次数，关闭连接
            log.info("心跳超时，关闭连接 - 用户ID: {}, Channel: {}", userId, ctx.channel().id());
            ctx.close();
        }
    }

    /**
     * 处理写空闲（服务器长时间没有发送消息）
     */
    private void handleWriterIdle(ChannelHandlerContext ctx) {
        // 主动发送心跳 ping
        String heartbeat = "{\"type\":\"HEARTBEAT\",\"content\":\"ping\",\"timestamp\":" + System.currentTimeMillis() + "}";
        ctx.writeAndFlush(new TextWebSocketFrame(heartbeat));

        log.debug("发送心跳 ping - Channel: {}", ctx.channel().id());
    }

    /**
     * 消息读取时重置心跳丢失计数
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 重置心跳丢失计数
        setMissCount(ctx, 0);
        super.channelRead(ctx, msg);
    }

    /**
     * 获取用户ID
     */
    private String getUserId(ChannelHandlerContext ctx) {
        try {
            Long userId = ctx.channel().attr(ChannelContextUtils.USER_ID_KEY).get();
            return userId != null ? userId.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 获取心跳丢失计数
     */
    private Integer getMissCount(ChannelHandlerContext ctx) {
        AttributeKey<Integer> key = AttributeKey.valueOf(HEARTBEAT_MISS_COUNT);
        Integer count = ctx.channel().attr(key).get();
        return count != null ? count : 0;
    }

    /**
     * 设置心跳丢失计数
     */
    private void setMissCount(ChannelHandlerContext ctx, Integer count) {
        AttributeKey<Integer> key = AttributeKey.valueOf(HEARTBEAT_MISS_COUNT);
        ctx.channel().attr(key).set(count);
    }
}
