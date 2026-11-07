package cn.cxdproject.coder.service;

import cn.cxdproject.coder.model.dto.SendFriendRequestDTO;
import cn.cxdproject.coder.model.dto.SendPrivateMessageDTO;
import cn.cxdproject.coder.model.vo.FriendRequestVO;
import cn.cxdproject.coder.model.vo.FriendSearchVO;
import cn.cxdproject.coder.model.vo.FriendVO;
import cn.cxdproject.coder.model.vo.PrivateMessageVO;

import java.util.List;

/**
 * 好友与私聊服务
 *
 * @author heathcetide
 */
public interface FriendService {

    /**
     * 按手机号或用户名搜索用户
     */
    List<FriendSearchVO> searchUsers(Long userId, String keyword);

    /**
     * 发送好友申请
     */
    void sendRequest(Long userId, SendFriendRequestDTO dto);

    /**
     * 我收到的待处理好友申请
     */
    List<FriendRequestVO> listReceivedRequests(Long userId);

    /**
     * 我发出的待处理好友申请
     */
    List<FriendRequestVO> listSentRequests(Long userId);

    /**
     * 审核好友申请
     */
    void reviewRequest(Long userId, Long requestId, boolean approve);

    /**
     * 我的好友列表
     */
    List<FriendVO> listFriends(Long userId);

    /**
     * 修改好友备注
     */
    void updateRemark(Long userId, Long friendId, String remark);

    /**
     * 私聊历史消息
     */
    List<PrivateMessageVO> listMessages(Long userId, Long friendId, Long beforeId, Integer limit);

    /**
     * 发送私聊消息
     */
    PrivateMessageVO sendMessage(Long userId, Long friendId, SendPrivateMessageDTO dto);
}
