package cn.cxdproject.coder;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication(exclude = {
    HibernateJpaAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class
})
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
@Slf4j
@MapperScan("cn.cxdproject.coder.mapper")
public class CampusApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(CampusApplication.class, args);
        printStartupInfo(context);
    }

    /**
     * 打印启动完成信息
     */
    private static void printStartupInfo(ApplicationContext context) {
        try {
            Environment env = context.getEnvironment();
            String applicationName = env.getProperty("code100.name");
            String port = env.getProperty("server.port", "8080");
            String path = env.getProperty("server.servlet.context-path", "");
            String nettyEnabled = env.getProperty("meeting.netty.enabled", "false");
            String nettyHost = env.getProperty("meeting.netty.host", "0.0.0.0");
            String nettyPort = env.getProperty("meeting.netty.port", "10086");
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            String normalizedPath = path == null ? "" : path;
            log.info("----------------------------------------------------------");
            log.info("{} 启动成功！", applicationName);
            log.info("应用名称：{}", applicationName);
            log.info("作者：{}", env.getProperty("code100.copyright.owner"));
            log.info("应用起始年份：{}", env.getProperty("code100.copyright.since-year"));
            log.info("应用许可证：{}", env.getProperty("code100.copyright.license"));
            log.info("本地访问：http://localhost:{}{}", port, normalizedPath);
            log.info("外部访问：http://{}:{}{}", hostAddress, port, normalizedPath);
            if (Boolean.parseBoolean(env.getProperty("code100.swagger.enabled", "false"))) {
                log.info("Swagger UI：http://localhost:{}{}/doc.html", port, normalizedPath);
            }
            log.info("Actuator：http://localhost:{}{}/actuator", port, normalizedPath);
            if (Boolean.parseBoolean(nettyEnabled)) {
                log.info("Netty 信令：tcp://{}:{}", nettyHost, nettyPort);
            }
            log.info("----------------------------------------------------------");
        } catch (UnknownHostException e) {
            log.warn("无法确定主机地址", e);
        }
    }

    /**
     * 配置国际化资源文件
     */
    @Bean
    public ReloadableResourceBundleMessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
}
