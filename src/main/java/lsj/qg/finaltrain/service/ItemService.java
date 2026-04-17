package lsj.qg.finaltrain.service;

import lsj.qg.finaltrain.pojo.ItemPost;
import lsj.qg.finaltrain.utils.ResultJson;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

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
    public boolean submitClaim(Long postId, String verificationAnswer);
    public List<Map<String, Object>> listOwnerClaims();
    public List<Map<String, Object>> listMyClaims();
    public boolean reviewClaim(Long claimId, Integer action, String feedback);
}
