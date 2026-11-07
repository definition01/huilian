package cn.cxdproject.coder.websockt;

import cn.cxdproject.coder.websockt.netty.NettyWebsocketStarter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class InitRun implements ApplicationRunner {

    @Resource
    private NettyWebsocketStarter nettyWebsocketStarter;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        new Thread(nettyWebsocketStarter).start();
    }
}
