package lsj.qg.finaltrain.controller;

import lsj.qg.finaltrain.mapper.UserMapper;
import lsj.qg.finaltrain.pojo.Message;
import lsj.qg.finaltrain.pojo.User;
import lsj.qg.finaltrain.service.impl.MessageServiceImpl;
import lsj.qg.finaltrain.utils.ResultJson;
import lsj.qg.finaltrain.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
@CrossOrigin(origins = "*") //开启跨域
public class MessageController {

    @Autowired
    private MessageServiceImpl messageServiceImpl;

    @Autowired
    private UserMapper userMapper;

    // 获取某个帖子的评论列表 (type=1)
    @GetMapping("/comments/{postId}")
    public ResultJson<List<Message>> getComments(@PathVariable Long postId) {
        List<Message> list = messageServiceImpl.getCommentsByPostId(postId);
        enrichMessageUserInfo(list);
        return ResultJson.success(list);
    }

    // 获取与某个人的私聊历史 (type=2)
    @GetMapping("/history/{friendId}")
    public ResultJson<List<Message>> getChatHistory(@PathVariable Long friendId) {
        Map<String,String> map = ThreadLocalUtil.get();
        Long userid = (long) Integer.parseInt(map.get("userid"));
        List<Message> list = messageServiceImpl.getHistory(userid, friendId);
        enrichMessageUserInfo(list);
        return ResultJson.success(list);
    }

    // 3. 统计未读消息数（用于在首页显示小红点）
    @GetMapping("/unread/count")
    public ResultJson<Long> getUnreadCount() {
        Map<String,String> map = ThreadLocalUtil.get();
        Long userid = (long) Integer.parseInt(map.get("userid"));
        return ResultJson.success(messageServiceImpl.countUnread(userid));
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
