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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Service
public class ItemServiceImpl implements ItemService {
    private static final Logger log = LoggerFactory.getLogger(ItemServiceImpl.class);

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
            String type = Integer.valueOf(1).equals(itemPost.getType()) ? "失物" : "拾取";
            String prompt = "请根据以下失物招领信息生成一段较为详细、客观、易读的AI对于物品的外观、大小或颜色的描述，控制在60字以内，仅输出描述文本，不要加前缀：\n"
                    + "类型：" + type + "\n"
                    + "物品名称：" + (itemPost.getItemName() == null ? "" : itemPost.getItemName()) + "\n"
                    + "地点：" + (itemPost.getLocation() == null ? "" : itemPost.getLocation()) + "\n"
                    + "时间：" + (itemPost.getEventTime() == null ? "" : itemPost.getEventTime()) + "\n"
                    + "描述：" + (itemPost.getDescription() == null ? "" : itemPost.getDescription());

            //设置限额
            //查询时间
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            // 查询今天已经调用了多少次
            LambdaQueryWrapper<AiUsageLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(AiUsageLog::getUserId, userId)
                    .ge(AiUsageLog::getCreateTime, todayStart);
            Long count = aiUsageLogMapper.selectCount(queryWrapper);
            // 判断是否超限
            if (count > 5) {
                return Flux.just("今日AI调用次数已达上限","[DONE]");
            }
            // 记录这次调用
            AiUsageLog log = new AiUsageLog();
            log.setUserId(userId);
            //时间可以让数据库自己生成
            aiUsageLogMapper.insert(log);
            Flux<String> ai = chatClient.prompt()
                                        .user(prompt)
                                        .stream()
                                        .content();
            // 将"[DONE]"添加到结果中((Flux.just("[DONE]")意思是创建一个包含一个元素"[DONE]"的Flux)
            return ai.concatWith(Flux.just("[DONE]"));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
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
