package cn.cxdproject.coder.model.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MessageSendDto {

    private Long meetingId;

    private Long senderId;

    private String senderName;

    private String messageType;

    private String content;

    private String targetScope;

    private Long recevedId;

    private LocalDateTime sentAt;

    private Long replyToId;

    private transient Long excludeUserId;

}
