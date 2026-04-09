package lsj.qg.finaltrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lsj.qg.finaltrain.mapper.ItemMapper;
import lsj.qg.finaltrain.mapper.ReportMapper;
import lsj.qg.finaltrain.pojo.ItemPost;
import lsj.qg.finaltrain.pojo.Report;
import lsj.qg.finaltrain.service.ItemService;
import lsj.qg.finaltrain.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;


@Service
public class ItemServiceImpl implements ItemService {
    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ReportMapper reportMapper;

    //发布失/拾取物信息
    //如果报错回滚事务
    @Transactional
    @Override
    public boolean InsertItem(ItemPost itemPost) {
        if (itemPost.getUserId() == null) {
            throw new NullPointerException("缺少发布者信息");
        }
        return itemMapper.insert(itemPost) > 0;
    }

    //信息浏览  1-(丢失)  2-(拾取)
    @Override
    public List<ItemPost> SelectByType(Integer type) {
        return itemMapper.selectList(new QueryWrapper<ItemPost>().eq("type", type));
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
        return itemMapper.selectList(new QueryWrapper<ItemPost>()
                .eq("user_id", userid)
                .eq("type", 1));
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


}
