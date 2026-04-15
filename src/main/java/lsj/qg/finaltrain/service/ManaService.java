package lsj.qg.finaltrain.service;

import lsj.qg.finaltrain.pojo.ItemPost;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ManaService {
    List<Map<String, Object>> listUsers(Long adminId, String keyword);

    void banUser(Long adminId, Long userId);

    void unbanUser(Long adminId, Long userId);

    void deletePost(Long adminId, Long postId);

    void deleteComment(Long adminId, Long commentId);

    List<Map<String, Object>> listReports(Long adminId, Integer status);

    void processReport(Long adminId, Long reportId);

    Long countPosts(Long adminId, Integer type, Integer status);

    Long countRecovered(Long adminId, Integer type);

    Long countActiveUsers(Long adminId, LocalDateTime start, LocalDateTime end);

    List<ItemPost> listPinRequests(Long adminId);

    void approvePin(Long adminId, Long postId);

    void rejectPin(Long adminId, Long postId);

    Flux<String> AiAnalyze();
}
