package com.gov.security.service;

import com.gov.security.entity.Message;
import com.gov.security.entity.User;
import com.gov.security.mapper.MessageMapper;
import com.gov.security.security.TokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final TokenManager tokenManager;
    private final UserService userService;

    /** 查询当前用户自己的留言 */
    public Map<String, Object> listMyMessages(String authHeader, int page, int size) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = tokenManager.getUserId(token);
        int offset = (page - 1) * size;
        List<Message> list = messageMapper.findByUserId(userId, size, offset);
        long total = messageMapper.countByUserId(userId);
        return Map.of("list", list, "total", total);
    }

    /** 提交留言（需登录） */
    public void submit(String authHeader, String content) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = tokenManager.getUserId(token);
        User user = userService.findById(userId);

        Message msg = new Message();
        msg.setUserId(userId);
        msg.setUserName(user.getUsername());
        msg.setRealName(user.getRealName());
        msg.setContent(content.trim());
        messageMapper.insert(msg);
    }

    /** 公开查询已审核通过的留言 */
    public Map<String, Object> listApproved(int page, int size) {
        int offset = (page - 1) * size;
        List<Message> list = messageMapper.findApproved(size, offset);
        long total = messageMapper.countApproved();
        return Map.of("list", list, "total", total);
    }

    /** 管理员分页查询，deleted=true 时查已删除 */
    public Map<String, Object> listForAdmin(Integer auditStatus, boolean deleted, int page, int size) {
        int offset = (page - 1) * size;
        List<Message> list = messageMapper.findPage(auditStatus, deleted, size, offset);
        long total = messageMapper.countPage(auditStatus, deleted);
        return Map.of("list", list, "total", total);
    }

    /** 审核通过 */
    public void approve(Long id) {
        messageMapper.updateAuditStatus(id, 1);
    }

    /** 审核拒绝 */
    public void reject(Long id) {
        messageMapper.updateAuditStatus(id, 2);
    }

    /** 回复留言 */
    public void reply(Long id, String reply) {
        messageMapper.reply(id, reply);
    }

    /** 删除留言 */
    public void delete(Long id) {
        messageMapper.delete(id);
    }
}
