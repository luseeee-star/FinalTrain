package lsj.qg.finaltrain.service;

import lsj.qg.finaltrain.pojo.Message;

import java.util.List;

public interface MessageService {
    public List<Message> getCommentsByPostId(Long postId);
    public List<Message> getHistory(Long userid, Long friendId);
    public Long countUnread(Long userid);
}
