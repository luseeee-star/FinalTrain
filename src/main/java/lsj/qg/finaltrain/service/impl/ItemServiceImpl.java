package lsj.qg.finaltrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lsj.qg.finaltrain.mapper.AiUsageLogMapper;
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
import java.util.List;
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

            //设置限额
            //查询时间
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            // 查询今天已经调用了多少次
            LambdaQueryWrapper<AiUsageLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AiUsageLog::getUserId, userId)
                    .ge(AiUsageLog::getCreateTime, todayStart);
            Long count = aiUsageLogMapper.selectCount(queryWrapper);
            // 判断是否超限
            if (count > 50) {
                return Flux.just("今日AI调用次数已达上限","[DONE]");
            }
            // 记录这次调用
            AiUsageLog log = new AiUsageLog();
            log.setUserId(userId);
            //时间可以让数据库自己生成
            aiUsageLogMapper.insert(log);

            // 有图则走多模态（先一次性生成，再以 SSE 形式返回）
            if (itemPost.getImageUrl() != null && !itemPost.getImageUrl().trim().isEmpty()) {
                String ai = generateAiDescription(itemPost);
                return Flux.just(ai == null ? "" : ai, "[DONE]");
            }

            // 无图则保留原流式输出
            Flux<String> ai = chatClient.prompt()
                    .user(prompt)
                    .stream()
                    .content();
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
     * 同步生成 AI 描述：有图 -> Qwen-VL 多模态；无图 -> 文本模型。
     */
    private String generateAiDescription(ItemPost itemPost) throws Exception {
        if (chatClient == null) return null;
        if (itemPost == null) return null;

        String prompt = buildAiPrompt(itemPost, true);
        String rawImageUrl = itemPost.getImageUrl();
        if (rawImageUrl == null || rawImageUrl.trim().isEmpty()) {
            return chatClient.prompt().user(prompt).call().content();
        }

        // 优先使用本地文件（避免 DashScope 无法访问 localhost 的 URL）
        UrlResource imageResource = null;
        String trimmed = rawImageUrl.trim();
        if (trimmed.startsWith("/msg/") && !trimmed.contains("..")) {
            String filename = trimmed.substring("/msg/".length());
            File f = new File(LOCAL_MSG_DIR + filename);
            if (f.exists() && f.isFile()) {
                imageResource = new UrlResource(f.toURI().toURL());
            }
        }
        // 如果不是本地 msg 文件（或文件不存在），再尝试用可访问的 URL
        if (imageResource == null) {
            String imageUrl = rawImageUrl.trim();
            if (!(imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
                return chatClient.prompt().user(prompt).call().content();
            }
            imageResource = new UrlResource(URI.create(imageUrl).toURL());
        }

        MimeType mime = guessImageMimeType(rawImageUrl);
        List<Media> mediaList = List.of(new Media(mime, imageResource));

        UserMessage message = new UserMessage(prompt, mediaList);

        Prompt chatPrompt = new Prompt(
                message,
                DashScopeChatOptions.builder()
                        .withModel("qwen-vl-max-latest")
                        .withMultiModel(true)
                        .build()
        );
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
        return itemMapper.selectById(postId);
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

}
