package lsj.qg.finaltrain.service;

import lsj.qg.finaltrain.pojo.Message;

import java.util.List;
import java.util.Map;

public interface MessageService {
    List<Message> getCommentsByPostId(Long postId);

    List<Message> getHistory(Long userId, Long friendId);

    Long countUnread(Long userId);

    List<Map<String, Object>> getChatSessions(Long userId);

    void deleteSession(Long userId, Long friendId);

    void markAsRead(Long userId, Long friendId);
}
