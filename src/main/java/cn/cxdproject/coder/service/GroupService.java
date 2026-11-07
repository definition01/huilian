package cn.cxdproject.coder.service;

import cn.cxdproject.coder.model.dto.AddGroupMembersDTO;
import cn.cxdproject.coder.model.dto.ApplyJoinGroupDTO;
import cn.cxdproject.coder.model.dto.CreateGroupDTO;
import cn.cxdproject.coder.model.dto.InviteGroupMembersDTO;
import cn.cxdproject.coder.model.dto.SendGroupMessageDTO;
import cn.cxdproject.coder.model.vo.GroupJoinRequestVO;
import cn.cxdproject.coder.model.vo.GroupMemberVO;
import cn.cxdproject.coder.model.vo.GroupMessageVO;
import cn.cxdproject.coder.model.vo.GroupSearchVO;
import cn.cxdproject.coder.model.vo.GroupVO;
import cn.cxdproject.coder.model.vo.GroupAppealVO;
import cn.cxdproject.coder.model.dto.GroupAppealDTO;
import cn.cxdproject.coder.model.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 群聊服务接口
 *
 * @author heathcetide
 */
public interface GroupService {

    /**
     * 创建群聊
     */
    GroupVO createGroup(Long userId, CreateGroupDTO dto);

    /**
     * 查询我加入的群聊
     */
    List<GroupVO> listMyGroups(Long userId);

    /**
     * 查询群聊详情
     */
    GroupVO getGroupDetail(Long userId, Long groupId);

    /**
     * 查询群成员
     */
    List<GroupMemberVO> listMembers(Long userId, Long groupId);

    /**
     * 添加群成员（仅群主）
     */
    void addMembers(Long userId, Long groupId, AddGroupMembersDTO dto);

    /**
     * 邀请好友入群：群主直接加入，普通成员邀请需群主审核
     */
    void inviteMembers(Long userId, Long groupId, InviteGroupMembersDTO dto);

    /**
     * 移除群成员（仅群主）
     */
    void removeMember(Long userId, Long groupId, Long targetUserId);

    /**
     * 解散群聊（仅群主）
     */
    void dissolveGroup(Long userId, Long groupId);

    /**
     * 退出群聊（群主需先解散）
     */
    void leaveGroup(Long userId, Long groupId);

    /**
     * 分页查询群消息（按消息ID向前翻页）
     */
    List<GroupMessageVO> listMessages(Long userId, Long groupId, Long beforeId, Integer limit);

    /**
     * 发送群消息
     */
    GroupMessageVO sendMessage(Long userId, Long groupId, SendGroupMessageDTO dto);

    /**
     * 按群号搜索群聊（非成员也可搜索）
     */
    GroupSearchVO searchByGroupNo(Long userId, String groupNo);

    /**
     * 通过群号申请入群，需群主审核
     */
    void applyJoin(Long userId, ApplyJoinGroupDTO dto);

    /**
     * 查询待审核的入群申请（仅群主）
     */
    List<GroupJoinRequestVO> listJoinRequests(Long userId, Long groupId);

    /**
     * 审核入群申请（仅群主）
     */
    void reviewJoinRequest(Long userId, Long groupId, Long requestId, boolean approve);

    /**
     * 上传群聊附件（图片/文件）
     */
    FileVO uploadAttachment(Long userId, Long groupId, MultipartFile file);

    /**
     * 群主对封禁群聊提交申诉
     */
    void submitAppeal(Long userId, Long groupId, GroupAppealDTO dto);

    /**
     * 查询群聊最新一次申诉（仅群主）
     */
    GroupAppealVO getLatestAppeal(Long userId, Long groupId);
}
