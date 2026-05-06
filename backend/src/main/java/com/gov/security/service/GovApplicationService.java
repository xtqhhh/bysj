package com.gov.security.service;

import com.gov.security.entity.GovApplication;
import com.gov.security.entity.User;
import com.gov.security.mapper.GovApplicationMapper;
import com.gov.security.security.TokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class GovApplicationService {

    private final GovApplicationMapper mapper;
    private final TokenManager tokenManager;
    private final UserService userService;

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    /** 生成申请编号：GW + yyyyMMdd + 5位序号 */
    private String generateApplyNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("GW%s%05d", date, SEQ.getAndIncrement());
    }

    /** 用户提交申请 */
    public GovApplication submit(String authHeader, String type, String title, String description) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = tokenManager.getUserId(token);
        User user = userService.findById(userId);

        GovApplication app = new GovApplication();
        app.setApplyNo(generateApplyNo());
        app.setUserId(userId);
        app.setUserName(user.getUsername());
        app.setRealName(user.getRealName());
        app.setType(type);
        app.setTitle(title);
        app.setDescription(description);
        mapper.insert(app);
        return app;
    }

    /** 查询当前用户的申请列表 */
    public Map<String, Object> myApplications(String authHeader, int page, int size) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = tokenManager.getUserId(token);
        int offset = (page - 1) * size;
        List<GovApplication> list = mapper.findByUserId(userId, size, offset);
        long total = mapper.countByUserId(userId);
        return Map.of("list", list, "total", total);
    }

    /** 查询单条申请详情 */
    public GovApplication detail(Long id) {
        return mapper.findById(id);
    }

    /** 按申请编号查询（公开查询） */
    public GovApplication queryByApplyNo(String applyNo) {
        return mapper.findByApplyNo(applyNo);
    }

    /** 管理员分页查询 */
    public Map<String, Object> listForAdmin(Integer status, String type, int page, int size) {
        int offset = (page - 1) * size;
        List<GovApplication> list = mapper.findPage(status, type, size, offset);
        long total = mapper.countPage(status, type);
        return Map.of("list", list, "total", total);
    }

    /** 管理员审核 */
    public void review(Long id, int status, String remark) {
        mapper.updateStatus(id, status, remark);
    }

    /** 用户撤销申请（仅限待受理状态，且只能撤销自己的申请） */
    public void cancel(Long id, String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = tokenManager.getUserId(token);
        GovApplication app = mapper.findById(id);
        if (app == null || !app.getUserId().equals(userId)) {
            throw new com.gov.security.common.exception.BusinessException(
                com.gov.security.common.response.ErrorCode.ACCESS_DENIED);
        }
        if (app.getStatus() != 0) {
            throw new com.gov.security.common.exception.BusinessException(
                com.gov.security.common.response.ErrorCode.ACCESS_DENIED, "只有待受理状态的申请可以撤销");
        }
        mapper.updateStatus(id, 5, "用户主动撤销");
    }
}
