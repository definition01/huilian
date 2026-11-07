package cn.cxdproject.coder.websockt.netty;

import cn.cxdproject.coder.config.JwtConfig;
import cn.cxdproject.coder.websockt.ChannelContextUtils;
import io.jsonwebtoken.Claims;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.channels.Channel;
import java.nio.charset.Charset;
import java.util.List;

@Component
@ChannelHandler.Sharable
@Slf4j
public class HandlerTokenValidation extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Resource
    private JwtConfig jwtConfig;

    @Resource
    private ChannelContextUtils channelContextUtils;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
       String url = fullHttpRequest.uri();
       QueryStringDecoder queryStringDecoder = new QueryStringDecoder(url);
       List<String> tokens = queryStringDecoder.parameters().get("token");
       if(tokens == null){
           sendErrorResponse(ctx);
           return;
       }
       String token = tokens.get(0);

        Claims claims = checkToken(token);
        if (claims == null){
            log.info("token校验失败");
            sendErrorResponse(ctx);
            return;
        }

        // 尝试多种方式获取用户ID
        String userIdStr = claims.getSubject();
        log.debug("JWT Claims - Subject: {}, Claims: {}", userIdStr, claims);

        // 如果subject为空，尝试从claims中的userId字段获取
        if (userIdStr == null || userIdStr.isEmpty()) {
            Object userIdObj = claims.get("userId");
            if (userIdObj != null) {
                userIdStr = String.valueOf(userIdObj);
                log.debug("从claims.userId获取到用户ID: {}", userIdStr);
            }
        }

        // 如果还是为空，尝试从claims中的id字段获取（兼容旧token）
        if (userIdStr == null || userIdStr.isEmpty()) {
            Object idObj = claims.get("id");
            if (idObj != null) {
                userIdStr = String.valueOf(idObj);
                log.debug("从claims.id获取到用户ID: {}", userIdStr);
            }
        }

        // 必须在放行请求（fireChannelRead）之前完成 userId 校验，
        // 否则 WebSocket 握手会先继续，再发 403 已经无效。
        if (userIdStr == null || userIdStr.isEmpty()) {
            log.error("token中没有用户ID信息");
            sendErrorResponse(ctx);
            return;
        }

        Long userId;
        try {
            userId = Long.valueOf(userIdStr);
        } catch (NumberFormatException e) {
            log.error("解析用户ID失败: {}", userIdStr, e);
            sendErrorResponse(ctx);
            return;
        }

        // 从握手查询参数中取会议ID：JWT 里的 meetingId 是登录时的快照，已失真
        List<String> meetingIds = queryStringDecoder.parameters().get("meetingId");
        String meetingId = (meetingIds != null && !meetingIds.isEmpty()) ? meetingIds.get(0) : null;

        // 群聊等纯消息通道：不加入会议房间，避免群聊页误改会议状态
        List<String> scenes = queryStringDecoder.parameters().get("scene");
        String scene = (scenes != null && !scenes.isEmpty()) ? scenes.get(0) : null;

        if ("chat".equals(scene)) {
            channelContextUtils.addChatContext(userId, ctx.channel());
            log.info("WebSocket token验证成功（群聊通道），用户ID: {}", userId);
        } else {
            channelContextUtils.addContext(userId, ctx.channel(), meetingId);
            log.info("WebSocket token验证成功，用户ID: {}, 会议ID: {}", userId, meetingId);
        }

        // 校验通过后才放行请求，让后续 handler 继续 WebSocket 握手
        //String cleanPath = queryStringDecoder.path();
        //fullHttpRequest.setUri(cleanPath);
        ctx.fireChannelRead(fullHttpRequest.retain());
    }

    private Claims checkToken(String token){
        if(token == null){
            return null;
        }
        Claims claims = jwtConfig.parseAndValidate(token);
        return claims;
    }

    private void sendErrorResponse(ChannelHandlerContext ctx){
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.FORBIDDEN, Unpooled.copiedBuffer("token无效", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE,"text/plain ;charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH,response.content().readableBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
