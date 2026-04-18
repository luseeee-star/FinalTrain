package lsj.qg.finaltrain.controller;

import lsj.qg.finaltrain.mapper.UserMapper;
import lsj.qg.finaltrain.pojo.Message;
import lsj.qg.finaltrain.pojo.User;
import lsj.qg.finaltrain.service.MessageService;
import lsj.qg.finaltrain.utils.ResultJson;
import lsj.qg.finaltrain.utils.ThreadLocalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
@CrossOrigin(origins = "*") //开启跨域
public class MessageController {
    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserMapper userMapper;

    // 获取某个帖子的评论列表 (type=1)
    @GetMapping("/comments/{postId}")
    public ResultJson<List<Message>> getComments(@PathVariable Long postId) {
        List<Message> list = messageService.getCommentsByPostId(postId);
        enrichMessageUserInfo(list);
        return ResultJson.success(list);
    }

    // 获取与某个人的私聊历史 (type=2)
    @GetMapping("/history/{friendId}")
    public ResultJson<List<Message>> getChatHistory(@PathVariable Long friendId) {
        Map<String,Object> map = ThreadLocalUtil.get();
        Long userid = Long.parseLong(String.valueOf(map.get("userid")));
        log.info("正在查询历史消息: userid={}, friendId={}", userid, friendId);
        List<Message> list = messageService.getHistory(userid, friendId);
        log.info("查询到历史消息数量: {}", (list != null ? list.size() : 0));
        enrichMessageUserInfo(list);
        return ResultJson.success(list);
    }

    // 3. 统计未读消息数（首页显示小红点）
    @GetMapping("/unread/count")
    public ResultJson<Long> getUnreadCount() {
        Map<String,Object> map = ThreadLocalUtil.get();
        Long userid = Long.parseLong(String.valueOf(map.get("userid")));
        return ResultJson.success(messageService.countUnread(userid));
    }

    // 获取聊天会话列表 (type=2)
    @GetMapping("/sessions")
    public ResultJson<List<Map<String, Object>>> getChatSessions() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Long userid = Long.parseLong(String.valueOf(map.get("userid")));
        List<Map<String, Object>> list = messageService.getChatSessions(userid);
        return ResultJson.success(list);
    }

    // 删除某个会话及其所有聊天记录
    @DeleteMapping("/sessions/{friendId}")
    public ResultJson<String> deleteSession(@PathVariable Long friendId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Long userid = Long.parseLong(String.valueOf(map.get("userid")));
        messageService.deleteSession(userid, friendId);
        log.info("用户 {} 删除了与 {} 的所有聊天记录", userid, friendId);
        return ResultJson.success("删除成功");
    }

    // 标记与某人的所有消息为已读
    @PostMapping("/read/{friendId}")
    public ResultJson<String> markAsRead(@PathVariable Long friendId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Long userid = Long.parseLong(String.valueOf(map.get("userid")));
        messageService.markAsRead(userid, friendId);
        log.info("用户 {} 标记与 {} 的消息为已读", userid, friendId);
        return ResultJson.success("已标记为已读");
    }

    private void enrichMessageUserInfo(List<Message> messages) {
        if (messages == null) {
            return;
        }
        for (Message msg : messages) {
            if (msg == null || msg.getSenderId() == null) {
                continue;
            }
            User user = userMapper.selectById(msg.getSenderId());
            if (user == null) {
                continue;
            }
            String nickname = user.getNickname();
            if (nickname == null || nickname.trim().isEmpty()) {
                nickname = user.getUsername();
            }
            msg.setSenderName(nickname);
            msg.setSenderAvatarUrl(user.getAvatarUrl());
        }
    }
}
