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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint(value = "/chat")
@Component
public class ChatEndpoint {
    private static final Logger log = LoggerFactory.getLogger(ChatEndpoint.class);
    private static final Map<Long, Session> onlineUsers = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        try {
            //通过握手协议时获得token数据
            String queryString = session.getQueryString();
            if (queryString == null || !queryString.contains("token=")) {
                System.err.println("WebSocket 连接失败: 未提供 Token");
                session.close();
                return;
            }
            
            String token = queryString.split("token=")[1].split("&")[0];
            Map<String, Object> claims = JwtUtil.verifyToken(token);
            if (claims == null || claims.get("userid") == null) {
                System.err.println("WebSocket 连接失败: Token 校验不通过");
                session.close();
                return;
            }
            
            Long userId = Long.parseLong(String.valueOf(claims.get("userid")));

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
            log.info("用户 {} (ID:{}) 已上线", displayName, userId);

        } catch (Exception e) {
            log.error("WebSocket 连接建立失败: ", e);
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
            //判断显示真名还是昵称
            if (sender != null) {
                String senderName = (sender.getNickname() != null && !sender.getNickname().isEmpty())
                        ? sender.getNickname()
                        : sender.getUsername();
                msg.setSenderName(senderName);
                msg.setSenderAvatarUrl(sender.getAvatarUrl());
            }

            //存入数据库
            MessageMapper messageMapper = SpringContextUtil.getBean(MessageMapper.class);
            messageMapper.insert(msg);

            // 转发消息给接收者
            Session receiverSession = onlineUsers.get(msg.getReceiverId());
            if (receiverSession != null && receiverSession.isOpen()) {
                receiverSession.getBasicRemote().sendText(JSON.toJSONString(msg));
                log.info("已转发消息给用户 {}", msg.getReceiverId());
            }

            // 同时也发回给发送者自己（确认发送成功）
            if (session.isOpen()) {
                session.getBasicRemote().sendText(JSON.toJSONString(msg));
            }
        } catch (Exception e) {
            log.error("处理 WebSocket 消息失败: ", e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        // 从 session 中取出刚才存进去的 userId
        Object uid = session.getUserProperties().get("userId");
        if (uid != null) {
            Long userId = (Long) uid;
            // 只有当这个 session 确实是当前保存在 Map 中的那个时才移除
            // 防止旧连接关闭时误删了新连接
            if (onlineUsers.get(userId) == session) {
                onlineUsers.remove(userId);
                log.info("用户 {} 下线了，当前在线人数: {}", userId, onlineUsers.size());
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 发生错误: ", error);
    }
}
