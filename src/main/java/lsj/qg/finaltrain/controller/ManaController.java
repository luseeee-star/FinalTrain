package lsj.qg.finaltrain.controller;

import lsj.qg.finaltrain.pojo.ItemPost;
import lsj.qg.finaltrain.service.ManaService;
import lsj.qg.finaltrain.utils.ResultJson;
import lsj.qg.finaltrain.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ManaItems")
@CrossOrigin(origins = "*")
public class ManaController {

    @Autowired
    private ManaService manaService;

    // 列出用户，支持可选的关键字搜索
    @GetMapping("/users")
    public ResultJson<List<Map<String, Object>>> listUsers(@RequestParam(value = "keyword", required = false) String keyword) {
        try {
            Long adminId = getCurrentUserId();
            return ResultJson.success(manaService.listUsers(adminId, keyword));
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    // 封禁用户
    @PatchMapping("/users/{userId}/ban")
    public ResultJson<String> banUser(@PathVariable Long userId) {
        try {
            Long adminId = getCurrentUserId();
            manaService.banUser(adminId, userId);
            return ResultJson.success("封禁成功", null);
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    // 解封用户
    @PatchMapping("/users/{userId}/unban")
    public ResultJson<String> unbanUser(@PathVariable Long userId) {
        try {
            Long adminId = getCurrentUserId();
            manaService.unbanUser(adminId, userId);
            return ResultJson.success("解封成功", null);
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    // 删除帖子
    @DeleteMapping("/posts/{postId}")
    public ResultJson<String> deletePost(@PathVariable Long postId) {
        try {
            Long adminId = getCurrentUserId();
            manaService.deletePost(adminId, postId);
            return ResultJson.success("删除成功", null);
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    // 删除评论
    @DeleteMapping("/comments/{commentId}")
    public ResultJson<String> deleteComment(@PathVariable Long commentId) {
        try {
            Long adminId = getCurrentUserId();
            manaService.deleteComment(adminId, commentId);
            return ResultJson.success("删除成功", null);
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    @GetMapping("/reports")
    public ResultJson<List<Map<String, Object>>> listReports(@RequestParam(value = "status", required = false) Integer status) {
        try {
            Long adminId = getCurrentUserId();
            return ResultJson.success(manaService.listReports(adminId, status));
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    @PatchMapping("/reports/{reportId}/process")
    public ResultJson<String> processReport(@PathVariable Long reportId) {
        try {
            Long adminId = getCurrentUserId();
            manaService.processReport(adminId, reportId);
            return ResultJson.success("处理成功", null);
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    // 获取统计信息，包括帖子数量、找回数量、活跃用户数量等
    @GetMapping("/stats")
    public ResultJson<Map<String, Object>> stats(
            @RequestParam(value = "type", required = false) Integer type,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end) {
        try {
            Long adminId = getCurrentUserId();

            LocalDateTime startTime;
            LocalDateTime endTime;
            if (start != null && end != null) {
                startTime = LocalDateTime.parse(start);
                endTime = LocalDateTime.parse(end);
            } else {
                endTime = LocalDateTime.now();
                startTime = endTime.minusDays(3);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("postCount", manaService.countPosts(adminId, type, status));
            data.put("recoveredCount", manaService.countRecovered(adminId, type));
            data.put("activeUserCount", manaService.countActiveUsers(adminId, startTime, endTime));
            data.put("activeStart", startTime);
            data.put("activeEnd", endTime);
            return ResultJson.success(data);
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    // 列出置顶请求
    @GetMapping("/pins/requests")
    public ResultJson<List<ItemPost>> listPinRequests() {
        try {
            Long adminId = getCurrentUserId();
            return ResultJson.success(manaService.listPinRequests(adminId));
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    // 批准置顶请求
    @PatchMapping("/pins/{postId}/approve")
    public ResultJson<String> approvePin(@PathVariable Long postId) {
        try {
            Long adminId = getCurrentUserId();
            manaService.approvePin(adminId, postId);
            return ResultJson.success("审核通过", null);
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    // 驳回置顶请求
    @PatchMapping("/pins/{postId}/reject")
    public ResultJson<String> rejectPin(@PathVariable Long postId) {
        try {
            Long adminId = getCurrentUserId();
            manaService.rejectPin(adminId, postId);
            return ResultJson.success("已驳回", null);
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    @GetMapping(value = "/AiAnalyze", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> AiAnalyze() {
        return manaService.AiAnalyze();
    }

    // 从ThreadLocal中获取当前用户ID
    private Long getCurrentUserId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        return Long.parseLong(String.valueOf(map.get("userid")));
    }
}
