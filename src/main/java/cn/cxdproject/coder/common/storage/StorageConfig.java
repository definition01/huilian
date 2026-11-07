package cn.cxdproject.coder.common.storage;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static cn.cxdproject.coder.common.constants.Constants.STORAGE_TYPE_COS;
import static cn.cxdproject.coder.common.constants.Constants.STORAGE_TYPE_LOCAL;



/**
 * 存储服务配置
 */
@Configuration
public class StorageConfig {

    /**
     * 本地存储服务
     */
    private final LocalStorageService localStorageService;

    /**
     * COS 存储服务（仅当 type=cos 时存在）
     */
    private final ObjectProvider<OssStorageService> ossStorageServiceProvider;

    /**
     * 文件存储属性
     */
    private final FileStorageProperties properties;

    public StorageConfig(LocalStorageService localStorageService,
                         ObjectProvider<OssStorageService> ossStorageServiceProvider,
                         FileStorageProperties properties) {
        this.localStorageService = localStorageService;
        this.ossStorageServiceProvider = ossStorageServiceProvider;
        this.properties = properties;
    }

    /**
     * 获取文件存储服务
     */
    @Bean
    @Primary
    public FileStorageAdapter fileStorageAdapter() {
        return switch (properties.getType()) {
            case STORAGE_TYPE_LOCAL -> localStorageService;
            case STORAGE_TYPE_COS -> ossStorageServiceProvider.getIfAvailable(() -> {
                throw new IllegalArgumentException("COS 存储服务未启用，请检查 code100.storage.type 配置");
            });
            default -> throw new IllegalArgumentException("不支持的存储类型: " + properties.getType());
        };
    }
}