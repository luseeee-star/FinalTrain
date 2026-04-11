package lsj.qg.finaltrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lsj.qg.finaltrain.mapper.MessageMapper;
import lsj.qg.finaltrain.mapper.UserMapper;
import lsj.qg.finaltrain.pojo.Message;
import lsj.qg.finaltrain.pojo.User;
import lsj.qg.finaltrain.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<Message> getCommentsByPostId(Long postId) {
        if (postId == null) {
            throw new NullPointerException("postId null");
        }
        return messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getPostId, postId)
                        .eq(Message::getType, 1)
                        .orderByAsc(Message::getCreateTime)
        );
    }

    /**
     * 获取与某个好友的完整聊天历史记录
     * 用于点击联系人后，在右侧聊天区域展示所有气泡
     */
    @Override
    public List<Message> getHistory(Long userid, Long friendId) {
        if (userid == null || friendId == null) {
            throw new NullPointerException("userid/friendId null");
        }

        // 1. 将该好友发给我的消息全部标记为已读
        messageMapper.update(null,
                new LambdaUpdateWrapper<Message>()
                        .set(Message::getIsRead, 1)
                        .eq(Message::getType, 2)
                        .eq(Message::getSenderId, friendId)
                        .eq(Message::getReceiverId, userid)
                        .eq(Message::getIsRead, 0)
        );

        // 2. 查询完整的历史记录并返回
        return messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getType, 2) // 过滤私聊类型
                        // 核心逻辑：查询 (我发给TA) 或者 (TA发给我) 的所有消息
                        .and(w -> w.nested(x -> x.eq(Message::getSenderId, userid).eq(Message::getReceiverId, friendId))
                                   .or(x -> x.eq(Message::getSenderId, friendId).eq(Message::getReceiverId, userid)))
                        .orderByAsc(Message::getCreateTime) // 按时间正序排列，确保对话逻辑连贯
        );
    }

    /**
     * 统计用户的未读私信总数
     * 用于首页导航栏显示小红点数字
     */
    @Override
    public Long countUnread(Long userid) {
        if (userid == null) {
            throw new NullPointerException("userid null");
        }
        return messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getType, 2)
                        // 只有发给我的才叫未读
                        .eq(Message::getReceiverId, userid) 
                        // 0代表未读
                        .eq(Message::getIsRead, 0)           
        );
    }

    /**
     * 获取最近联系人会话列表
     * 用于左侧联系人列表展示：谁联系了我、最后说了什么、有几个未读
     */
    @Override
    public List<Map<String, Object>> getChatSessions(Long userid) {
        if (userid == null) {
            throw new NullPointerException("userid is null");
        }

        // 1. 查出所有与我相关的私聊消息，按时间倒序（最新的在前）
        List<Message> list = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getType, 2)
                        .and(w -> w
                                .eq(Message::getSenderId, userid)
                                .or()
                                .eq(Message::getReceiverId, userid))
                        .orderByDesc(Message::getCreateTime)
        );

        // 2. 内存分组处理：每个联系人只保留最新的一条消息
        // 使用 LinkedHashMap 保证插入顺序（即保持时间倒序）
        LinkedHashMap<Long, Map<String, Object>> sessions = new LinkedHashMap<>();
        for (Message msg : list) {
            // 确定谁是“对方”
            Long friendId = userid.equals(msg.getSenderId()) ? msg.getReceiverId() : msg.getSenderId();
            if (friendId == null) {
                continue;
            }

            Map<String, Object> session = sessions.get(friendId);
            if (session == null) {
                // 如果这个联系人还没处理过，由于是倒序遍历，当前这条就是该联系人的“最后一条消息”
                User friend = userMapper.selectById(friendId);
                String displayName = getDisplayName(friend, friendId);

                session = new LinkedHashMap<>();
                session.put("friendId", friendId);
                session.put("friendName", displayName);
                session.put("friendAvatarUrl", friend != null ? friend.getAvatarUrl() : null);
                session.put("lastContent", msg.getContent());
                session.put("lastTime", msg.getCreateTime());
                session.put("unreadCount", 0L);
                sessions.put(friendId, session);
            }

            // 3. 统计该会话中发给我的未读消息数
            if (userid.equals(msg.getReceiverId()) && Integer.valueOf(0).equals(msg.getIsRead())) {
                Long unread = (Long) session.get("unreadCount");
                session.put("unreadCount", unread + 1L);
            }
        }

        return new ArrayList<>(sessions.values());
    }

    /**
     * 删除与某个联系人的所有会话记录
     */
    @Override
    public void deleteSession(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            throw new NullPointerException("userId/friendId is null");
        }
        // 删除所有 A 发给 B 和 B 发给 A 的私聊消息 (type=2)
        messageMapper.delete(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getType, 2)
                        .and(w -> w
                                .nested(x -> x
                                        .eq(Message::getSenderId, userId)
                                        .eq(Message::getReceiverId, friendId))
                                   .or(x -> x
                                           .eq(Message::getSenderId, friendId)
                                           .eq(Message::getReceiverId, userId)))
        );
    }

    /**
     * 辅助方法：获取用户的显示名称（优先昵称，其次用户名）
     */
    private String getDisplayName(User user, Long userId) {
        if (user == null) return "用户" + userId;
        if (user.getNickname() != null && !user.getNickname().trim().isEmpty()) {
            return user.getNickname();
        }
        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            return user.getUsername();
        }
        return "用户" + userId;
    }

}
