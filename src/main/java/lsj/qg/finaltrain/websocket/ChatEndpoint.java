package lsj.qg.finaltrain.websocket;

import com.alibaba.fastjson.JSON;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lsj.qg.finaltrain.mapper.MessageMapper;
import lsj.qg.finaltrain.mapper.UserMapper;
import lsj.qg.finaltrain.pojo.User;
import lsj.qg.finaltrain.utils.JwtUtil;
import lsj.qg.finaltrain.utils.SpringContextUtil;
import lsj.qg.finaltrain.pojo.Message;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/chat")
@Component
public class ChatEndpoint {
    private static final Map<Long, Session> onlineUsers = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        try {
            //通过握手协议时获得token数据
            String queryString = session.getQueryString();
            String token = queryString.split("token=")[1].split("&")[0];
            Object userIdObj = JwtUtil.verifyToken(token).get("userid");
            Long userId = Long.parseLong(String.valueOf(userIdObj));

            // 获取名字并绑定
            // 从 Spring 容器拿到 UserMapper
            UserMapper userMapper = SpringContextUtil.getBean(UserMapper.class);
            User user = userMapper.selectById(userId);

            // 有昵称用昵称，没昵称用用户名
            String displayName = (user.getNickname() != null && !user.getNickname().isEmpty())
                    ? user.getNickname()
                    : user.getUsername();

            // 把 userId 和 displayName 都存进当前 session 的“兜里”
            session.getUserProperties().put("userId", userId);
            session.getUserProperties().put("senderName", displayName);

            onlineUsers.put(userId, session);
            System.out.println("用户 " + displayName + " (ID:" + userId + ") 已上线");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            //解析前端传回来的json
            Message msg = JSON.parseObject(message, Message.class);

            Long senderId = (Long) session.getUserProperties().get("userId");
            UserMapper userMapper = SpringContextUtil.getBean(UserMapper.class);
            User sender = userMapper.selectById(senderId);
            if (sender != null) {
                String senderName = (sender.getNickname() != null && !sender.getNickname().isEmpty())
                        ? sender.getNickname()
                        : sender.getUsername();
                msg.setSenderName(senderName);
                msg.setSenderAvatarUrl(sender.getAvatarUrl());
            }

            Session receiverSession = onlineUsers.get(msg.getReceiverId());

            //存入数据库
            MessageMapper messageMapper = SpringContextUtil.getBean(MessageMapper.class);
            messageMapper.insert(msg);

            if (receiverSession != null && receiverSession.isOpen()) {
                // 如果符合条件则发送消息
                receiverSession.getBasicRemote().sendText(JSON.toJSONString(msg));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session) {
        // 从 session 中取出刚才存进去的 userId
        Long userId = (Long) session.getUserProperties().get("userId");
        if (userId != null) {
            onlineUsers.remove(userId);
            System.out.println("用户 " + userId + " 下线了，当前在线人数: " + onlineUsers.size());
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("WebSocket 发生错误");
        error.printStackTrace();
    }
}
