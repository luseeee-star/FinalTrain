package lsj.qg.finaltrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lsj.qg.finaltrain.mapper.AiUsageLogMapper;
import lsj.qg.finaltrain.pojo.ClaimRequest;
import lsj.qg.finaltrain.mapper.ClaimRequestMapper;
import lsj.qg.finaltrain.mapper.ItemMapper;
import lsj.qg.finaltrain.mapper.ReportMapper;
import lsj.qg.finaltrain.pojo.AiUsageLog;
import lsj.qg.finaltrain.pojo.ItemPost;
import lsj.qg.finaltrain.pojo.Report;
import lsj.qg.finaltrain.service.ItemService;
import lsj.qg.finaltrain.utils.ThreadLocalUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.core.io.UrlResource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.net.URI;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;


@Service
public class ItemServiceImpl implements ItemService {
    private static final Logger log = LoggerFactory.getLogger(ItemServiceImpl.class);
    private static final String LOCAL_MSG_DIR = "D:\\Java\\FinalTrain\\msg\\";

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired(required = false)
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    private AiUsageLogMapper aiUsageLogMapper;

    @Autowired
    private ClaimRequestMapper claimRequestMapper;

    private ChatClient chatClient;

    @PostConstruct
    public void initChatClient() {
        if (chatClientBuilder != null) {
            chatClient = chatClientBuilder.build();
        }
    }


    //发布失物/拾取物信息
    //如果报错回滚事务
    @Transactional
    @Override
    public boolean InsertItem(ItemPost itemPost,Long userid) {
        if (itemPost.getUserId() == null) {
            throw new NullPointerException("缺少发布者信息");
        }
        boolean ok = itemMapper.insert(itemPost) > 0;
        if (!ok) {
            return false;
        }

        ItemPost update = new ItemPost();
        update.setId(itemPost.getId());
        update.setCreateTime(LocalDateTime.now());
        itemMapper.updateById(update);

        return true;
    }
    /**
     * 关于 Flux<String>：
     * - Flux 是 Spring Reactor 的响应式流，用于处理异步数据流
     * - 这里用来实现 SSE（Server-Sent Events），让前端能实时看到 AI 逐字输出的效果
     * - "[DONE]" 是特殊标记，告诉前端流已结束
     * 
     * 关于多模态 vs 流式：
     * - 多模态模型（qwen-vl）不支持流式输出，所以一次性生成后包装成 Flux
     * - 纯文本模型支持流式，所以用 .stream() 直接返回 Flux
     */
    @Override
    public Flux<String> AIdescription(ItemPost itemPost, Long userId){
        try {
            if (chatClient == null) {
                throw new NullPointerException("AI 服务未启用");
            }
            if (itemPost == null) {
                throw new NullPointerException("参数不能为空");
            }
            String prompt = buildAiPrompt(itemPost, true);

            // 获取今天 00:00:00 的时间点
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            // 查询今天该用户已经调用了多少次 AI
            LambdaQueryWrapper<AiUsageLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AiUsageLog::getUserId, userId)
                    .ge(AiUsageLog::getCreateTime, todayStart);
            Long count = aiUsageLogMapper.selectCount(queryWrapper);
            // 超过 20 次则拒绝服务
            if (count > 20) {
                return Flux.just("今日AI调用次数已达上限","[DONE]");
            }
            // 记录本次调用（用于下次限流统计）
            AiUsageLog log = new AiUsageLog();
            log.setUserId(userId);
            aiUsageLogMapper.insert(log);

            // ========== 有图 vs 无图 ==========
            // 有图片时：走多模态模型（一次性生成）
            if (itemPost.getImageUrl() != null && !itemPost.getImageUrl().trim().isEmpty()) {
                // generateAiDescription 内部会调用 qwen-vl-max-latest 模型识别图片
                String ai = generateAiDescription(itemPost);
                // 包装成 Flux 返回，保持和无图情况的接口一致性
                return Flux.just(ai == null ? "" : ai, "[DONE]");
            }

            // 无图片时：走纯文本模型（流式输出）
            // .stream() 开启流式模式，AI 会逐字返回，前端实时显示
            Flux<String> ai = chatClient.prompt()
                    .user(prompt)
                    .stream()
                    .content();
            // concatWith 在流末尾添加 "[DONE]" 标记
            return ai.concatWith(Flux.just("[DONE]"));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private String buildAiPrompt(ItemPost itemPost, boolean allowLonger) {
        String type = Integer.valueOf(1).equals(itemPost.getType()) ? "失物" : "拾取";
        String limit = allowLonger ? "控制在60字以内" : "控制在60字以内";
        return "请根据以下失物招领信息生成一段较为详细、客观、易读的AI对于物品的外观、大小或颜色的描述，" + limit + "，"
                + "仅输出描述文本，不要加前缀,不要泄露像密码、身份证号、门牌号等隐私：\n"
                + "类型：" + type + "\n"
                + "物品名称：" + (itemPost.getItemName() == null ? "" : itemPost.getItemName()) + "\n"
                + "地点：" + (itemPost.getLocation() == null ? "" : itemPost.getLocation()) + "\n"
                + "时间：" + (itemPost.getEventTime() == null ? "" : itemPost.getEventTime()) + "\n"
                + "描述：" + (itemPost.getDescription() == null ? "" : itemPost.getDescription());
    }

    /**
     * 关于图片处理的难点：
     * - DashScope（阿里云大模型服务）无法访问 localhost 或内网 URL
     * - 所以本地图片（/msg/xxx.jpg）需要转换为本地文件流读取
     * - 外部 URL（http/https）则直接通过网络访问
     * 
     * 关于多模态模型：
     * - qwen-vl-max-latest 是通义千问的视觉语言模型
     * - 可以同时理解图片内容和文本提示词
     * - 返回对图片中物品的描述
     */
    private String generateAiDescription(ItemPost itemPost) throws Exception {
        if (chatClient == null) return null;
        if (itemPost == null) return null;

        String prompt = buildAiPrompt(itemPost, true);
        String rawImageUrl = itemPost.getImageUrl();
        
        // 如果没有图片 URL，退化为纯文本调用
        if (rawImageUrl == null || rawImageUrl.trim().isEmpty()) {
            return chatClient.prompt().user(prompt).call().content();
        }

        // ========== 图片资源准备 ==========
        // 目标：将各种形式的图片 URL 转换为 Spring 的 UrlResource 对象
        UrlResource imageResource = null;
        String trimmed = rawImageUrl.trim();
        
        // 情况 1：本地图片路径（如 /msg/xxx.jpg）
        // 需要从本地文件系统读取，因为 DashScope 无法访问 localhost
        if (trimmed.startsWith("/msg/") && !trimmed.contains("..")) {
            // 安全检查：1. 必须以 /msg/ 开头  2. 不能包含 ..（防止目录遍历攻击）
            String filename = trimmed.substring("/msg/".length());
            File f = new File(LOCAL_MSG_DIR + filename);
            if (f.exists() && f.isFile()) {
                // 转换为 UrlResource，Spring AI 可以读取
                imageResource = new UrlResource(f.toURI().toURL());
            }
        }
        
        // 情况 2：外部可访问的 URL（http/https）
        if (imageResource == null) {
            String imageUrl = rawImageUrl.trim();
            // 如果不是 http/https 开头，说明格式不支持，退化为纯文本
            if (!(imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
                return chatClient.prompt().user(prompt).call().content();
            }
            // 通过网络 URL 创建资源
            imageResource = new UrlResource(URI.create(imageUrl).toURL());
        }

        // ========== 构建多模态请求 ==========
        // 1. 猜测图片的 MIME 类型（如 image/jpeg, image/png）
        MimeType mime = guessImageMimeType(rawImageUrl);
        
        // 2. 创建 Media 对象（封装图片数据和类型）
        List<Media> mediaList = List.of(new Media(mime, imageResource));
        
        // 3. 构建用户消息（包含文本提示词 + 图片列表）
        UserMessage message = new UserMessage(prompt, mediaList);

        // 4. 创建 Prompt，配置多模态模型参数
        Prompt chatPrompt = new Prompt(
                message,
                DashScopeChatOptions.builder()
                        .withModel("qwen-vl-max-latest")  // 通义千问视觉语言模型
                        .withMultiModel(true)              // 启用多模态模式
                        .build()
        );
        
        // 5. 调用 AI 并返回生成的描述文本
        return chatClient.prompt(chatPrompt).call().content();
    }

    private MimeType guessImageMimeType(String urlOrPath) {
        if (urlOrPath == null) return MimeTypeUtils.IMAGE_JPEG;
        String s = urlOrPath.toLowerCase();
        if (s.contains(".png")) return MimeTypeUtils.IMAGE_PNG;
        if (s.contains(".webp")) return MimeTypeUtils.parseMimeType("image/webp");
        if (s.contains(".gif")) return MimeTypeUtils.parseMimeType("image/gif");
        return MimeTypeUtils.IMAGE_JPEG;
    }

    //信息浏览  1-(丢失)  2-(拾取)
    @Override
    public List<ItemPost> SelectByType(Integer type) {
        return itemMapper.selectList(new LambdaQueryWrapper<ItemPost>().eq(ItemPost::getType, type));
    }

    //举报
    //如果报错回滚事务
    @Transactional
    @Override
    public boolean InsertReport(Report report) {
        if (report.getReporterId() == null) {
            throw new NullPointerException("缺少举报人ID");
        }
        if (report.getPostId() == null) {
            throw new NullPointerException("缺少被举报的帖子ID");
        }
        return reportMapper.insert(report) > 0;
    }

    //信息置顶
    // (先查询自己发布的丢失信息然后再选择)
    @Override
    public List<ItemPost> SelectById() {
        Map<String,String> map = ThreadLocalUtil.get();
        Long userid = (long) Integer.parseInt(map.get("userid"));
        return itemMapper.selectList(new LambdaQueryWrapper<ItemPost>()
                .eq(ItemPost::getUserId, userid)
                .eq(ItemPost::getType, 1));
    }

    // 更新数据
    //如果报错回滚事务
    @Transactional
    @Override
    public boolean UpdateItemPinned(Long postId) {
        if (postId == null) {
            throw new NullPointerException("缺少帖子ID");
        }
        Map<String,String> map = ThreadLocalUtil.get();
        Long userid = (long) Integer.parseInt(map.get("userid"));
        ItemPost dbPost = itemMapper.selectById(postId);
        if (dbPost == null) {
            throw new NullPointerException("帖子不存在");
        }
        if (!userid.equals(dbPost.getUserId())) {
            throw new RuntimeException("只能置顶自己的帖子");
        }
        if (!Integer.valueOf(1).equals(dbPost.getType())) {
            throw new RuntimeException("仅失物帖子可申请置顶");
        }
        ItemPost update = new ItemPost();
        update.setId(postId);
        update.setIsPinned(1); // 1=申请置顶
        return itemMapper.updateById(update) > 0;
    }

    @Override
    public ItemPost SelectPostById(Long postId) {
        if (postId == null) {
            throw new NullPointerException("缺少帖子ID");
        }
        ItemPost post = itemMapper.selectById(postId);
        if (post == null) {
            return null;
        }
        Map<String, String> map = ThreadLocalUtil.get();
        Long currentUserId = map == null ? null : Long.parseLong(map.get("userid"));
        boolean isOwner = currentUserId != null && currentUserId.equals(post.getUserId());
        boolean canViewContact = isOwner;
        Integer myClaimStatus = null;
        if (!isOwner && currentUserId != null) {
            ClaimRequest accepted = claimRequestMapper.selectOne(
                    new LambdaQueryWrapper<ClaimRequest>()
                            .eq(ClaimRequest::getItemPostId, postId)
                            .eq(ClaimRequest::getApplicantId, currentUserId)
                            .eq(ClaimRequest::getStatus, 1)
                            .last("limit 1")
            );
            if (accepted != null) {
                canViewContact = true;
                myClaimStatus = 1;
            } else {
                ClaimRequest mine = claimRequestMapper.selectOne(
                        new LambdaQueryWrapper<ClaimRequest>()
                                .eq(ClaimRequest::getItemPostId, postId)
                                .eq(ClaimRequest::getApplicantId, currentUserId)
                                .orderByDesc(ClaimRequest::getCreateTime)
                                .last("limit 1")
                );
                if (mine != null) {
                    myClaimStatus = mine.getStatus();
                }
            }
        }
        if (!canViewContact) {
            post.setContactInfo("审核通过后可见");
        }
        post.setCanViewContact(canViewContact);
        post.setMyClaimStatus(myClaimStatus);
        return post;
    }

    @Transactional
    @Override
    public boolean UpdateItem(ItemPost itemPost) {
        if (itemPost == null || itemPost.getId() == null) {
            throw new NullPointerException("缺少帖子ID");
        }
        Map<String,String> map = ThreadLocalUtil.get();
        Long userid = (long) Integer.parseInt(map.get("userid"));
        ItemPost dbPost = itemMapper.selectById(itemPost.getId());
        if (dbPost == null) {
            throw new NullPointerException("帖子不存在");
        }
        if (!userid.equals(dbPost.getUserId())) {
            throw new RuntimeException("只能修改自己的帖子");
        }
        itemPost.setUserId(userid);
        itemPost.setCreateTime(null);
        return itemMapper.updateById(itemPost) > 0;
    }

    @Transactional
    @Override
    public boolean DeleteItem(Long postId) {
        if (postId == null) {
            throw new NullPointerException("缺少帖子ID");
        }
        Map<String,String> map = ThreadLocalUtil.get();
        Long userid = (long) Integer.parseInt(map.get("userid"));
        ItemPost dbPost = itemMapper.selectById(postId);
        if (dbPost == null) {
            throw new NullPointerException("帖子不存在");
        }
        if (!userid.equals(dbPost.getUserId())) {
            throw new RuntimeException("只能删除自己的帖子");
        }
        return itemMapper.deleteById(postId) > 0;
    }

    // 标记为已找回
    //如果报错回滚事务
    @Transactional
    @Override
    public boolean SetFound(Long postId) {
        if (postId == null) {
            throw new NullPointerException("缺少帖子ID");
        }
        Map<String, String> map = ThreadLocalUtil.get();
        Long userid = (long) Integer.parseInt(map.get("userid"));
        ItemPost dbPost = itemMapper.selectById(postId);
        if (dbPost == null) {
            throw new NullPointerException("帖子不存在");
        }
        if (!userid.equals(dbPost.getUserId())) {
            throw new RuntimeException("只能标记自己的帖子为已找回");
        }
        ItemPost update = new ItemPost();
        update.setId(postId);
        update.setStatus(1); // 1=已完成
        return itemMapper.updateById(update) > 0;
    }

    @Transactional
    @Override
    public boolean submitClaim(Long postId, String verificationAnswer) {
        if (postId == null) {
            throw new NullPointerException("缺少帖子ID");
        }
        if (verificationAnswer == null || verificationAnswer.trim().isEmpty()) {
            throw new IllegalArgumentException("请填写核验答案");
        }
        Map<String, String> map = ThreadLocalUtil.get();
        Long applicantId = Long.parseLong(map.get("userid"));
        ItemPost post = itemMapper.selectById(postId);
        if (post == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        if (applicantId.equals(post.getUserId())) {
            throw new IllegalArgumentException("不能申请自己的帖子");
        }
        ClaimRequest existed = claimRequestMapper.selectOne(
                new LambdaQueryWrapper<ClaimRequest>()
                        .eq(ClaimRequest::getItemPostId, postId)
                        .eq(ClaimRequest::getApplicantId, applicantId)
                        .in(ClaimRequest::getStatus, 0, 1, 3)
                        .last("limit 1")
        );
        if (existed != null) {
            throw new IllegalArgumentException("你已有待处理申请，请勿重复提交");
        }
        ClaimRequest claim = new ClaimRequest();
        claim.setItemPostId(postId);
        claim.setApplicantId(applicantId);
        claim.setOwnerId(post.getUserId());
        claim.setVerificationAnswer(verificationAnswer.trim());
        claim.setStatus(0);
        claim.setCreateTime(LocalDateTime.now());
        claim.setUpdateTime(LocalDateTime.now());
        return claimRequestMapper.insert(claim) > 0;
    }

    @Override
    public List<Map<String, Object>> listOwnerClaims() {
        Map<String, String> map = ThreadLocalUtil.get();
        Long ownerId = Long.parseLong(map.get("userid"));
        List<ClaimRequest> claims = claimRequestMapper.selectList(
                new LambdaQueryWrapper<ClaimRequest>()
                        .eq(ClaimRequest::getOwnerId, ownerId)
                        .orderByDesc(ClaimRequest::getCreateTime)
        );
        return buildClaimRows(claims);
    }

    @Override
    public List<Map<String, Object>> listMyClaims() {
        Map<String, String> map = ThreadLocalUtil.get();
        Long applicantId = Long.parseLong(map.get("userid"));
        List<ClaimRequest> claims = claimRequestMapper.selectList(
                new LambdaQueryWrapper<ClaimRequest>()
                        .eq(ClaimRequest::getApplicantId, applicantId)
                        .orderByDesc(ClaimRequest::getCreateTime)
        );
        return buildClaimRows(claims);
    }

    @Transactional
    @Override
    public boolean reviewClaim(Long claimId, Integer action, String feedback) {
        if (claimId == null) {
            throw new NullPointerException("缺少申请ID");
        }
        if (action == null || action < 1 || action > 3) {
            throw new IllegalArgumentException("审批动作无效");
        }
        Map<String, String> map = ThreadLocalUtil.get();
        Long ownerId = Long.parseLong(map.get("userid"));
        ClaimRequest claim = claimRequestMapper.selectById(claimId);
        if (claim == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        if (!ownerId.equals(claim.getOwnerId())) {
            throw new IllegalArgumentException("只能处理自己帖子的申请");
        }
        if (!Integer.valueOf(0).equals(claim.getStatus()) && !Integer.valueOf(3).equals(claim.getStatus())) {
            throw new IllegalArgumentException("该申请已处理，不能重复审批");
        }
        return claimRequestMapper.update(null, new LambdaUpdateWrapper<ClaimRequest>()
                .set(ClaimRequest::getStatus, action)
                .set(ClaimRequest::getRejectionReason, feedback == null ? null : feedback.trim())
                .eq(ClaimRequest::getId, claimId)) > 0;
    }

    private List<Map<String, Object>> buildClaimRows(List<ClaimRequest> claims) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ClaimRequest claim : claims) {
            if (claim == null) {
                continue;
            }
            ItemPost post = claim.getItemPostId() == null ? null : itemMapper.selectById(claim.getItemPostId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", claim.getId());
            row.put("itemPostId", claim.getItemPostId());
            row.put("applicantId", claim.getApplicantId());
            row.put("ownerId", claim.getOwnerId());
            row.put("verificationAnswer", claim.getVerificationAnswer());
            row.put("status", claim.getStatus());
            row.put("feedback", claim.getRejectionReason());
            row.put("createTime", claim.getCreateTime());
            row.put("updateTime", claim.getUpdateTime());
            if (post != null) {
                row.put("itemName", post.getItemName());
                row.put("itemType", post.getType());
                row.put("itemLocation", post.getLocation());
            }
            rows.add(row);
        }
        return rows;
    }

}
