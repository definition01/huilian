package cn.cxdproject.coder.controller;

import cn.cxdproject.coder.common.ApiResponse;
import cn.cxdproject.coder.common.context.AuthContext;
import cn.cxdproject.coder.model.dto.ChatStateDTO;
import cn.cxdproject.coder.service.ChatStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 会话已读与置顶控制器
 *
 * @author heathcetide
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatStateService chatStateService;

    public ChatController(ChatStateService chatStateService) {
        this.chatStateService = chatStateService;
    }

    /**
     * 标记会话已读
     */
    @PostMapping("/read")
    public ApiResponse<Void> markRead(@Valid @RequestBody ChatStateDTO dto) {
        chatStateService.markRead(AuthContext.getCurrentUser().getId(), dto.getType(), dto.getTargetId());
        return ApiResponse.success(null);
    }

    /**
     * 设置/取消会话置顶
     */
    @PostMapping("/pin")
    public ApiResponse<Void> setPinned(@Valid @RequestBody ChatStateDTO dto) {
        chatStateService.setPinned(AuthContext.getCurrentUser().getId(), dto.getType(), dto.getTargetId(),
                dto.getPinned());
        return ApiResponse.success(null);
    }
}
