package lsj.qg.finaltrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lsj.qg.finaltrain.mapper.MessageMapper;
import lsj.qg.finaltrain.pojo.Message;
import lsj.qg.finaltrain.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public List<Message> getCommentsByPostId(Long postId) {
        if (postId == null) {
            throw new NullPointerException("postId is null");
        }
        return messageMapper.selectList(
                new QueryWrapper<Message>()
                        .eq("post_id", postId)
                        .eq("type", 1)
                        .orderByAsc("create_time")
        );
    }

    @Override
    public List<Message> getHistory(Long userid,Long friendId){
        if (userid == null || friendId == null) {
            throw new NullPointerException("userid/friendId is null");
        }
        return messageMapper.selectList(
                new QueryWrapper<Message>()
                        .eq("type", 2)
                        .and(w -> w
                                .and(x -> x.eq("sender_id", userid).eq("receiver_id", friendId))
                                .or()
                                .and(x -> x.eq("sender_id", friendId).eq("receiver_id", userid)))
                        .orderByAsc("create_time")
        );
    }

    @Override
    public Long countUnread(Long userid) {
        if (userid == null) {
            throw new NullPointerException("userid is null");
        }
        return messageMapper.selectCount(
                new QueryWrapper<Message>()
                        .eq("type", 2)
                        .eq("receiver_id", userid)
                        .eq("is_read", 0)
        );
    }

}
