package lsj.qg.finaltrain.service;

import lsj.qg.finaltrain.pojo.ItemPost;
import lsj.qg.finaltrain.utils.ResultJson;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ItemService {
    public boolean InsertItem(ItemPost itemPost,Long userid);
    public List<ItemPost> SelectByType(Integer type);
    public boolean InsertReport(lsj.qg.finaltrain.pojo.Report report);
    public List<ItemPost> SelectById();
    public boolean UpdateItemPinned(Long postId);
    public ItemPost SelectPostById(Long postId);
    public boolean UpdateItem(ItemPost itemPost);
    public boolean DeleteItem(Long postId);
    public Flux<String> AIdescription(ItemPost itemPost, Long userId);
    public boolean SetFound(Long postId);
}
