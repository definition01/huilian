package cn.cxdproject.coder.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageSendVo {

    private Long meetingId;

    private String senderName;

    private String messageType;

    private String content;

    private String targetScope;

    private Long recevedId;

    private LocalDateTime sentAt;

    private Long replyToId;

}
