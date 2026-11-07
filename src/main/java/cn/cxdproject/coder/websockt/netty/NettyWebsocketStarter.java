package cn.cxdproject.coder.websockt.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

@Component
@Slf4j
public class NettyWebsocketStarter implements Runnable {

    @Value("${meeting.netty.port:10086}")
    private Integer nettyPort;

    @Value("${meeting.netty.boss-threads:1}")
    private Integer bossThreads;

    @Value("${meeting.netty.worker-threads:0}")
    private Integer workerThreads;

    @Value("${meeting.netty.so-backlog:1024}")
    private Integer soBacklog;

    @Value("${meeting.netty.keep-alive:true}")
    private Boolean keepAlive;

    @Value("${meeting.netty.tcp-no-delay:true}")
    private Boolean tcpNoDelay;

    @Value("${meeting.netty.heartbeat-enabled:true}")
    private Boolean heartbeatEnabled;

    @Value("${meeting.netty.reader-idle-seconds:90}")
    private Integer readerIdleSeconds;

    @Value("${meeting.netty.writer-idle-seconds:30}")
    private Integer writerIdleSeconds;

    @Value("${meeting.netty.all-idle-seconds:0}")
    private Integer allIdleSeconds;

    @Value("${meeting.netty.max-heartbeat-misses:3}")
    private Integer maxHeartbeatMisses;

    @Value("${meeting.netty.max-frame-length:8192}")
    private Integer maxFrameLength;

    @Resource
    private HandlerTokenValidation handlerTokenValidation;

    @Resource
    private HandlerWebsocket handlerWebsocket;

    /**
     * boss线程，用于处理连接
     */
    private EventLoopGroup bossGroup;

    /**
     * work线程，用于处理消息
     */
    private EventLoopGroup workGroup;

    @PostConstruct
    public void init() {
        int boss = bossThreads != null ? bossThreads : 1;
        int worker = workerThreads != null && workerThreads > 0 ? workerThreads : 0;
        this.bossGroup = new NioEventLoopGroup(boss);
        this.workGroup = worker > 0 ? new NioEventLoopGroup(worker) : new NioEventLoopGroup();
        log.info("Netty WebSocket 配置 - 端口: {}, boss: {}, worker: {}, readerIdle: {}, writerIdle: {}, maxMiss: {}",
                nettyPort, boss, worker > 0 ? worker : "default", readerIdleSeconds, writerIdleSeconds, maxHeartbeatMisses);
    }

    @Override
    public void run() {
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .option(ChannelOption.SO_BACKLOG, soBacklog != null ? soBacklog : 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, keepAlive != null ? keepAlive : true)
                    .childOption(ChannelOption.TCP_NODELAY, tcpNoDelay != null ? tcpNoDelay : true)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            //对http协议的支持，使用http的编码器和解码器
                            pipeline.addLast(new HttpServerCodec());
                            //http的消息聚合器，主要是将分片的http消息聚合成完整FullHttpRequest
                            pipeline.addLast(new HttpObjectAggregator(64 * 1024));

                            //心跳处理器
                            if (Boolean.TRUE.equals(heartbeatEnabled)) {
                                pipeline.addLast(new IdleStateHandler(
                                        readerIdleSeconds != null ? readerIdleSeconds : 90,
                                        writerIdleSeconds != null ? writerIdleSeconds : 30,
                                        allIdleSeconds != null ? allIdleSeconds : 0));
                                pipeline.addLast(new HandlerHeartBeat(maxHeartbeatMisses != null ? maxHeartbeatMisses : 3));
                            }

                            //鉴权进行token校验，拦截channelRead事件
                            pipeline.addLast(handlerTokenValidation);
                            //websocket协议处理器
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true,
                                    maxFrameLength != null ? maxFrameLength : 8192, true, true, 10000L));
                            pipeline.addLast(handlerWebsocket);
                        }
                    });

            Channel channel = serverBootstrap.bind(nettyPort).sync().channel();
            log.info(" Netty WebSocket 服务器启动成功 - 端口: {}", nettyPort);
            channel.closeFuture().sync();
        } catch (Exception e) {
            log.error(" Netty WebSocket 服务器启动失败 - 端口: {}", nettyPort, e);
        } finally {
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            if (workGroup != null) {
                workGroup.shutdownGracefully();
            }
        }
    }

    @PreDestroy
    public void close() {
        log.info("正在关闭 Netty WebSocket 服务器...");
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workGroup != null) {
            workGroup.shutdownGracefully();
        }
    }
}
