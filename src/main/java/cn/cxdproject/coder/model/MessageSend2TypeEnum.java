package cn.cxdproject.coder.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageSend2TypeEnum {
    /**
     * 公共消息：发送给会议内的所有人
     */
    GROUP("group", "群发消息"),

    /**
     * 私聊消息：仅发送者和指定的接收者可见
     */
    PRIVATE("private", "私聊消息"),

    /**
     * 主持人消息：仅发送给当前会议的主持人/管理员
     */
    HOST("host", "主持人消息");

    /**
     * 数据库中实际存储的值
     */
    private final String code;

    /**
     * 业务描述信息
     */
    private final String desc;

}
