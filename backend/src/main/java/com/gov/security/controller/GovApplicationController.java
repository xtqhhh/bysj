package com.gov.security.controller;

import com.gov.security.common.response.ApiResponse;
import com.gov.security.entity.GovApplication;
import com.gov.security.service.GovApplicationService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class GovApplicationController {

    private final GovApplicationService service;

    /** 用户撤销申请（仅限待受理状态） */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancel(@PathVariable Long id,
                                     @RequestHeader("Authorization") String auth) {
        service.cancel(id, auth);
        return ApiResponse.success("申请已撤销");
    }

    /** 用户提交申请 */
    @PostMapping
    public ApiResponse<GovApplication> submit(@RequestHeader("Authorization") String auth,
                                               @RequestBody SubmitRequest req) {
        return ApiResponse.success(service.submit(auth, req.getType(), req.getTitle(), req.getDescription()));
    }

    /** 查询我的申请列表 */
    @GetMapping("/my")
    public ApiResponse<Map<String, Object>> myList(@RequestHeader("Authorization") String auth,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "5") int size) {
        return ApiResponse.success(service.myApplications(auth, page, size));
    }

    /** 查询申请详情 */
    @GetMapping("/{id}")
    public ApiResponse<GovApplication> detail(@PathVariable Long id) {
        return ApiResponse.success(service.detail(id));
    }

    /** 按申请编号查询（门户公开查询） */
    @GetMapping("/query")
    public ApiResponse<GovApplication> queryByNo(@RequestParam String applyNo) {
        GovApplication app = service.queryByApplyNo(applyNo);
        if (app == null) return ApiResponse.error("NOT_FOUND", "未找到该申请编号");
        return ApiResponse.success(app);
    }

    /** 管理员查询所有申请 */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('OFFICER')")
    public ApiResponse<Map<String, Object>> adminList(@RequestParam(required = false) Integer status,
                                                       @RequestParam(required = false) String type,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(service.listForAdmin(status, type, page, size));
    }

    /** 管理员审核申请 */
    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('OFFICER')")
    public ApiResponse<Void> review(@PathVariable Long id, @RequestBody ReviewRequest req) {
        service.review(id, req.getStatus(), req.getRemark());
        return ApiResponse.success("审核完成");
    }

    @Data
    public static class SubmitRequest {
        @NotBlank private String type;
        @NotBlank private String title;
        private String description;
    }

    @Data
    public static class ReviewRequest {
        private int status;
        private String remark;
    }
}
