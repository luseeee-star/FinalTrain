package lsj.qg.finaltrain.service;

import lsj.qg.finaltrain.pojo.ItemPost;

import java.util.List;

public interface ItemService {
    public boolean InsertItem(ItemPost itemPost);
    public List<ItemPost> SelectByType(Integer type);
    public boolean InsertReport(lsj.qg.finaltrain.pojo.Report report);
    public List<ItemPost> SelectById();
    public boolean UpdateItemPinned(Long postId);
    public ItemPost SelectPostById(Long postId);
    public boolean UpdateItem(ItemPost itemPost);
    public boolean DeleteItem(Long postId);
}
