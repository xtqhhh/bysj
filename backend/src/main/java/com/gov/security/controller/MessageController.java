package com.gov.security.controller;

import com.gov.security.common.response.ApiResponse;
import com.gov.security.service.MessageService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /** 登录用户查询自己的留言 */
    @GetMapping("/my")
    public ApiResponse<Map<String, Object>> myMessages(
            @RequestHeader("Authorization") String auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ApiResponse.success(messageService.listMyMessages(auth, page, size));
    }

    /** 公开：查询已审核通过的留言列表 */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(messageService.listApproved(page, size));
    }

    /** 登录用户：提交留言 */
    @PostMapping
    public ApiResponse<Void> submit(@RequestHeader("Authorization") String auth,
                                     @RequestBody SubmitRequest req) {
        messageService.submit(auth, req.getContent());
        return ApiResponse.success("留言提交成功，等待管理员审核");
    }

    /** 管理员：查询所有留言（可按审核状态过滤，deleted=true 查已删除） */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Map<String, Object>> adminList(
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(defaultValue = "false") String deleted,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        boolean isDeleted = "true".equalsIgnoreCase(deleted);
        return ApiResponse.success(messageService.listForAdmin(auditStatus, isDeleted, page, size));
    }

    /** 管理员：审核通过 */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> approve(@PathVariable Long id) {
        messageService.approve(id);
        return ApiResponse.success("审核通过");
    }

    /** 管理员：审核拒绝 */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> reject(@PathVariable Long id) {
        messageService.reject(id);
        return ApiResponse.success("已拒绝");
    }

    /** 管理员：回复留言 */
    @PutMapping("/{id}/reply")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> reply(@PathVariable Long id, @RequestBody ReplyRequest req) {
        messageService.reply(id, req.getReply());
        return ApiResponse.success("回复成功");
    }

    /** 管理员：删除留言 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        messageService.delete(id);
        return ApiResponse.success("删除成功");
    }

    @Data
    public static class SubmitRequest {
        @NotBlank(message = "留言内容不能为空")
        @Size(max = 500, message = "留言内容不能超过500字")
        private String content;
    }

    @Data
    public static class ReplyRequest {
        @NotBlank(message = "回复内容不能为空")
        private String reply;
    }
}
