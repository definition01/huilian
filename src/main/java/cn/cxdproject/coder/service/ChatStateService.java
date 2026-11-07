package cn.cxdproject.coder.service;

import cn.cxdproject.coder.model.entity.ImChatState;

import java.util.List;

/**
 * 会话已读与置顶服务
 *
 * @author heathcetide
 */
public interface ChatStateService {

    /**
     * 标记会话已读（已读到该会话最新的消息）
     */
    void markRead(Long userId, String type, Long targetId);

    /**
     * 设置/取消会话置顶
     */
    void setPinned(Long userId, String type, Long targetId, Boolean pinned);

    /**
     * 查询用户某一类会话的已读与置顶状态
     */
    List<ImChatState> listStates(Long userId, String type);
}
