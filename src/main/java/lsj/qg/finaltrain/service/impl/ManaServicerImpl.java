package lsj.qg.finaltrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.PostConstruct;
import lsj.qg.finaltrain.mapper.ItemMapper;
import lsj.qg.finaltrain.mapper.MessageMapper;
import lsj.qg.finaltrain.mapper.ReportMapper;
import lsj.qg.finaltrain.mapper.UserMapper;
import lsj.qg.finaltrain.pojo.ItemPost;
import lsj.qg.finaltrain.pojo.Message;
import lsj.qg.finaltrain.pojo.Report;
import lsj.qg.finaltrain.pojo.User;
import lsj.qg.finaltrain.service.ManaService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ManaServicerImpl implements ManaService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired(required = false)
    private ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;

    @PostConstruct
    public void initChatClient() {
        if (chatClientBuilder != null) {
            chatClient = chatClientBuilder.build();
        }
    }

    // 列出用户，支持关键字搜索
    @Override
    public List<Map<String, Object>> listUsers(Long adminId, String keyword) {
        ensureAdmin(adminId);
        // 构建查询条件，按ID倒序(DESC就是倒序排
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().orderByDesc(User::getId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            // 构建模糊查询条件，匹配用户名、昵称、电话或邮箱
            wrapper.and(w -> w
                    .like(User::getUsername, kw)
                    .or()
                    .like(User::getNickname, kw)
                    .or()
                    .like(User::getPhone, kw)
                    .or()
                    .like(User::getEmail, kw));
        }
        List<User> users = userMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        // 遍历用户列表，构建返回的Map
        for (User u : users) {
            if (u == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", u.getId());
            row.put("username", u.getUsername());
            row.put("nickname", u.getNickname());
            row.put("avatarUrl", u.getAvatarUrl());
            row.put("email", u.getEmail());
            row.put("phone", u.getPhone());
            row.put("role", u.getRole());
            row.put("status", u.getStatus());
            row.put("lastLoginTime", u.getLastLoginTime());
            result.add(row);
        }
        return result;
    }

    // 封禁用户
    @Override
    public void banUser(Long adminId, Long userId) {
        ensureAdmin(adminId);
        if (userId == null) {
            throw new NullPointerException("userId is null");
        }
        if (adminId.equals(userId)) {
            throw new IllegalArgumentException("不能封禁自己");
        }
        User update = new User();
        update.setId(userId);
        update.setStatus(1); // 设置状态为封禁
        if (userMapper.updateById(update) <= 0) {
            throw new IllegalArgumentException("封禁失败：用户不存在");
        }
    }

    // 解封用户
    @Override
    public void unbanUser(Long adminId, Long userId) {
        ensureAdmin(adminId);
        if (userId == null) {
            throw new NullPointerException("userId is null");
        }
        User update = new User();
        update.setId(userId);
        update.setStatus(0); // 设置状态为正常
        if (userMapper.updateById(update) <= 0) {
            throw new IllegalArgumentException("解封失败：用户不存在");
        }
    }

    // 删除帖子及其相关评论和举报
    @Override
    public void deletePost(Long adminId, Long postId) {
        ensureAdmin(adminId);
        if (postId == null) {
            throw new NullPointerException("postId is null");
        }

        // 删除帖子
        itemMapper.deleteById(postId);

        // 删除相关评论（type=1的消息）
        messageMapper.delete(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getType, 1)
                        .eq(Message::getPostId, postId)
        );

        // 删除相关举报
        reportMapper.delete(
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getPostId, postId)
        );
    }

    // 删除评论
    @Override
    public void deleteComment(Long adminId, Long commentId) {
        ensureAdmin(adminId);
        if (commentId == null) {
            throw new NullPointerException("commentId is null");
        }
        // 删除指定评论（type=1的消息）
        messageMapper.delete(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getId, commentId)
                        .eq(Message::getType, 1)
        );
    }

    @Override
    public List<Map<String, Object>> listReports(Long adminId, Integer status) {
        ensureAdmin(adminId);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>().orderByDesc(Report::getId);
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        List<Report> list = reportMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Report r : list) {
            if (r == null) {
                continue;
            }
            User reporter = r.getReporterId() != null ? userMapper.selectById(r.getReporterId()) : null;
            String reporterName = reporter != null
                    ? ((reporter.getNickname() != null && !reporter.getNickname().trim().isEmpty()) ? reporter.getNickname() : reporter.getUsername())
                    : null;

            ItemPost post = r.getPostId() != null ? itemMapper.selectById(r.getPostId()) : null;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", r.getId());
            row.put("reporterId", r.getReporterId());
            row.put("reporterName", reporterName);
            row.put("postId", r.getPostId());
            row.put("reason", r.getReason());
            row.put("status", r.getStatus());
            row.put("postItemName", post != null ? post.getItemName() : null);
            row.put("postType", post != null ? post.getType() : null);
            row.put("postStatus", post != null ? post.getStatus() : null);
            row.put("postPinned", post != null ? post.getIsPinned() : null);
            row.put("postLocation", post != null ? post.getLocation() : null);
            row.put("postEventTime", post != null ? post.getEventTime() : null);
            row.put("postCreateTime", post != null ? post.getCreateTime() : null);
            result.add(row);
        }
        return result;
    }

    @Override
    public void processReport(Long adminId, Long reportId) {
        ensureAdmin(adminId);
        if (reportId == null) {
            throw new NullPointerException("reportId is null");
        }
        int updated = reportMapper.update(null,
                new LambdaUpdateWrapper<Report>()
                        .set(Report::getStatus, 1)
                        .eq(Report::getId, reportId)
        );
        if (updated <= 0) {
            throw new IllegalArgumentException("举报不存在");
        }
    }

    // 统计帖子数量
    @Override
    public Long countPosts(Long adminId, Integer type, Integer status) {
        ensureAdmin(adminId);
        LambdaQueryWrapper<ItemPost> wrapper = new LambdaQueryWrapper<ItemPost>();
        if (type != null) {
            wrapper.eq(ItemPost::getType, type); // 按类型过滤
        }
        if (status != null) {
            wrapper.eq(ItemPost::getStatus, status); // 按状态过滤
        }
        return itemMapper.selectCount(wrapper);
    }

    // 统计已找回物品数量
    @Override
    public Long countRecovered(Long adminId, Integer type) {
        ensureAdmin(adminId);
        // 查询状态为1（已找回）的帖子
        LambdaQueryWrapper<ItemPost> wrapper = new LambdaQueryWrapper<ItemPost>()
                .eq(ItemPost::getStatus, 1);
        if (type != null) {
            wrapper.eq(ItemPost::getType, type); // 按类型过滤
        }
        return itemMapper.selectCount(wrapper);
    }

    // 统计活跃用户数量
    @Override
    public Long countActiveUsers(Long adminId, LocalDateTime start, LocalDateTime end) {
        ensureAdmin(adminId);
        if (start == null || end == null) {
            throw new NullPointerException("start/end is null");
        }
        // 查询最后登录时间在指定范围内的用户
        return userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .ge(User::getLastLoginTime, start)
                        .le(User::getLastLoginTime, end)
        );
    }

    // 列出置顶申请
    @Override
    public List<ItemPost> listPinRequests(Long adminId) {
        ensureAdmin(adminId);
        // 查询isPinned=1的帖子，按创建时间倒序
        List<ItemPost> list = itemMapper.selectList(
                new LambdaQueryWrapper<ItemPost>()
                        .eq(ItemPost::getIsPinned, 1)
                        .orderByDesc(ItemPost::getCreateTime)
        );
        // 循环填充用户信息
        for (ItemPost post : list) {
            if (post == null || post.getUserId() == null) {
                continue;
            }
            User u = userMapper.selectById(post.getUserId());
            if (u == null) {
                continue;
            }
            String nickname = u.getNickname();
            if (nickname == null || nickname.trim().isEmpty()) {
                nickname = u.getUsername(); // 如果昵称为空，使用用户名
            }
            post.setUserNickname(nickname);
            post.setUserAvatarUrl(u.getAvatarUrl());
        }
        return list;
    }

    // 批准置顶申请
    @Override
    public void approvePin(Long adminId, Long postId) {
        ensureAdmin(adminId);
        updatePinnedStatus(postId, 2); // 设置为已置顶
    }

    // 驳回置顶申请
    @Override
    public void rejectPin(Long adminId, Long postId) {
        ensureAdmin(adminId);
        updatePinnedStatus(postId, 0); // 设置为未置顶
    }

    // 更新置顶状态
    private void updatePinnedStatus(Long postId, Integer target) {
        if (postId == null) {
            throw new NullPointerException("postId is null");
        }
        ItemPost post = itemMapper.selectById(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        if (!Integer.valueOf(1).equals(post.getIsPinned())) {
            throw new IllegalArgumentException("该帖子没有待处理的置顶申请");
        }
        // 更新置顶状态
        itemMapper.update(null,
                new LambdaUpdateWrapper<ItemPost>()
                        .set(ItemPost::getIsPinned, target)
                        .eq(ItemPost::getId, postId)
        );
    }

    // 验证管理员权限
    private void ensureAdmin(Long adminId) {
        if (adminId == null) {
            throw new NullPointerException("adminId is null");
        }
        User admin = userMapper.selectById(adminId);
        if (admin == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!Integer.valueOf(1).equals(admin.getRole())) {
            throw new IllegalArgumentException("无权限"); // 角色不为1（管理员）
        }
        if (!Integer.valueOf(0).equals(admin.getStatus())) {
            throw new IllegalArgumentException("账号已被封禁"); // 状态不为0（正常）
        }
    }

    @Override
    public Flux<String> AiAnalyze(){
        //统计丢失物品数量
        QueryWrapper<ItemPost> wrapper1 = new QueryWrapper<>();

        // 只查地点字段，并统计每个地点的数量，重命名为total_count
        wrapper1.select("location", "COUNT(1) as total_count")
                .ge("create_time", LocalDateTime.now().minusDays(3))//筛选最近3天
                .groupBy("location");
        // 执行查询并存入map中
        List<Map<String, Object>> LostStats = itemMapper.selectMaps(wrapper1);
        // 按数量倒序排列
        LostStats.sort((o1, o2) -> {
            Long count1 = Long.valueOf(o1.get("total_count").toString());
            Long count2 = Long.valueOf(o2.get("total_count").toString());
            return count2.compareTo(count1); // 倒序
        });

        //再对物品名称进行同样的操作
        QueryWrapper<ItemPost> wrapper2 = new QueryWrapper<>();
        wrapper2.select("item_name", "COUNT(1) as total_count")
                .ge("create_time", LocalDateTime.now().minusDays(3))
                .groupBy("item_name");
        List<Map<String, Object>> ItemStats = itemMapper.selectMaps(wrapper2);

        ItemStats.sort((o1, o2) -> {
            Long count1 = Long.valueOf(o1.get("total_count").toString());
            Long count2 = Long.valueOf(o2.get("total_count").toString());
            return count2.compareTo(count1); // 倒序
        });
        //整合失物数据信息
        StringBuilder sb = new StringBuilder("最近一周失物数据统计如下：\n");
        for (Map<String, Object> item : LostStats) {
            sb.append("地点：").append(item.get("location"))
                    .append("，丢失数量：").append(item.get("total_count"))
                    .append("\n");
        }
        //整合失物类型信息
        StringBuilder sb1 = new StringBuilder("最近一周失物类型统计如下：\n");
        for (Map<String, Object> item : ItemStats) {
            sb1.append("物品类型：").append(item.get("item_name"))
                    .append("，丢失数量：").append(item.get("total_count"))
                    .append("\n");
        }

        if (chatClient == null) {
            throw new NullPointerException("AI 服务未启用");
        }
        String prompt = "请根据以下关于丢失物品地点与名称的信息生成一段简短、客观、易读的总结给管理员阅读," +
                "你要根据信息加自己判断说出哪个地方丢失物品多以及哪个物品丢失数量多" +
                "仅输出描述文本，不要加前缀：\n"
                + "丢失地点信息：" + sb+"\n" + "丢失物品类型信息:"+ sb1;

        Flux<String> ai = chatClient.prompt()
                .user(prompt)
                .stream()
                .content();
        return ai.concatWith(Flux.just("[DONE]"));
    }
}
