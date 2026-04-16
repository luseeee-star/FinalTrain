package lsj.qg.finaltrain.controller;

import lsj.qg.finaltrain.mapper.ItemMapper;
import lsj.qg.finaltrain.mapper.UserMapper;
import lsj.qg.finaltrain.pojo.ItemPost;
import lsj.qg.finaltrain.pojo.Report;
import lsj.qg.finaltrain.pojo.User;
import lsj.qg.finaltrain.service.ItemService;
import lsj.qg.finaltrain.utils.ResultJson;
import lsj.qg.finaltrain.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/StuItems")
@CrossOrigin(origins = "*")
public class StuItemController {
    private static final String IMAGE_DIR = "D:\\Java\\FinalTrain\\msg\\";

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ItemMapper itemMapper;


    @PostMapping("/insertItem")
    public ResultJson<String> insertItem(
            @RequestParam Map<String, String> form,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            //根据token获取id
            Map<String, String> map = ThreadLocalUtil.get();
            Long userid = (long) Integer.parseInt(map.get("userid"));
            String imageUrl = null;

            // 判断是否有文件，没文件则是null
            if (file != null && !file.isEmpty()) {
                String originalfileName = file.getOriginalFilename();
                String ext = ".jpg";
                if (originalfileName != null && originalfileName.lastIndexOf('.') >= 0) {
                    ext = originalfileName.substring(originalfileName.lastIndexOf('.'));
                }
                // 将文件随机命名
                String fileName = UUID.randomUUID() + ext;
                File dir = new File(IMAGE_DIR);
                //如果没有文件夹就创建
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                file.transferTo(new File(IMAGE_DIR + fileName));
                imageUrl = "/msg/" + fileName;
            } else {
                imageUrl = form.get("imageUrl");
            }

            // 把前端数据存储并存入数据库
            ItemPost itemPost = new ItemPost();
            itemPost.setUserId(userid);
            itemPost.setType(form.get("type") == null ? 1 : Integer.parseInt(form.get("type")));
            itemPost.setItemName(form.get("itemName"));
            itemPost.setLocation(form.get("location"));
            itemPost.setEventTime(LocalDateTime.parse(form.get("eventTime")));
            itemPost.setDescription(form.get("description"));
            itemPost.setContactInfo(form.get("contactInfo"));
            itemPost.setStatus(form.get("status") == null ? 0 : Integer.parseInt(form.get("status")));
            itemPost.setIsPinned(form.get("isPinned") == null ? 0 : Integer.parseInt(form.get("isPinned")));
            itemPost.setImageUrl(imageUrl);

            itemService.InsertItem(itemPost,userid);
            return ResultJson.success("添加成功");
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    // 生成 AI 描述（手动触发，支持携带图片文件）
    @PostMapping(value = "/ai/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> CreateAiDescription(
            @RequestParam Map<String, String> form,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            Map<String, String> map = ThreadLocalUtil.get();
            Long userId = (long) Integer.parseInt(map.get("userid"));

            ItemPost itemPost = new ItemPost();
            itemPost.setType(form.get("type") == null ? 1 : Integer.parseInt(form.get("type")));
            itemPost.setItemName(form.get("itemName"));
            itemPost.setLocation(form.get("location"));
            if (form.get("eventTime") != null && !form.get("eventTime").trim().isEmpty()) {
                itemPost.setEventTime(LocalDateTime.parse(form.get("eventTime")));
            }
            itemPost.setDescription(form.get("description"));
            itemPost.setImageUrl(form.get("imageUrl"));

            // 预览时如带文件，先临时落到 /msg，供 AI 多模态读取（不入库）
            if (file != null && !file.isEmpty()) {
                String originalfileName = file.getOriginalFilename();
                String ext = ".jpg";
                if (originalfileName != null && originalfileName.lastIndexOf('.') >= 0) {
                    ext = originalfileName.substring(originalfileName.lastIndexOf('.'));
                }
                String fileName = UUID.randomUUID() + ext;
                File dir = new File(IMAGE_DIR);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                file.transferTo(new File(IMAGE_DIR + fileName));
                itemPost.setImageUrl("/msg/" + fileName);
            }

            return itemService.AIdescription(itemPost, userId);
        } catch (Exception e) {
            return Flux.just("生成失败：" + e.getMessage(), "[DONE]");
        }
    }

    @GetMapping("/selectByType")
    public ResultJson<List<ItemPost>> selectByType(Integer type) {
        try {
            List<ItemPost> list = itemService.SelectByType(type);
            enrichPostsUserInfo(list);
            return ResultJson.success(list);
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    @PostMapping("/reports")
    public ResultJson<String> insertReports(@RequestBody Report report) {
        try {
            Map<String, String> map = ThreadLocalUtil.get();
            Long userid = (long) Integer.parseInt(map.get("userid"));
            report.setReporterId(userid);
            itemService.InsertReport(report);
            return ResultJson.success("举报成功");
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    @GetMapping("/selectById")
    public ResultJson<List<ItemPost>> selectById() {
        try {
            List<ItemPost> list = itemService.SelectById();
            enrichPostsUserInfo(list);
            return ResultJson.success(list);
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    @GetMapping("/detail/{postId}")
    public ResultJson<ItemPost> selectDetailById(@PathVariable Long postId) {
        try {
            ItemPost post = itemService.SelectPostById(postId);
            enrichPostUserInfo(post);
            return ResultJson.success(post);
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    @PatchMapping("/updatepinned/{postId}")
    public ResultJson<String> updatePinned(@PathVariable Long postId) {
        try {
            itemService.UpdateItemPinned(postId);
            return ResultJson.success("发送申请成功");
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    @PutMapping("/updateItem")
    public ResultJson<String> updateItem(@RequestBody ItemPost itemPost) {
        try {
            itemService.UpdateItem(itemPost);
            return ResultJson.success("更新成功");
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    @DeleteMapping("/deleteItem/{postId}")
    public ResultJson<String> deleteItem(@PathVariable Long postId) {
        try {
            itemService.DeleteItem(postId);
            return ResultJson.success("删除成功");
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    @PatchMapping("/updateFound/{postId}")
    public ResultJson<String> updateFound(@PathVariable Long postId) {
        try {
            itemService.SetFound(postId);
            return ResultJson.success("发送申请成功");
        } catch (Exception e) {
            return ResultJson.error(e.getMessage());
        }
    }

    private void enrichPostsUserInfo(List<ItemPost> posts) {
        if (posts == null) {
            return;
        }
        for (ItemPost post : posts) {
            enrichPostUserInfo(post);
        }
    }

    private void enrichPostUserInfo(ItemPost post) {
        if (post == null || post.getUserId() == null) {
            return;
        }
        User user = userMapper.selectById(post.getUserId());
        if (user == null) {
            return;
        }
        String nickname = user.getNickname();
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = user.getUsername();
        }
        post.setUserNickname(nickname);
        post.setUserAvatarUrl(user.getAvatarUrl());
    }

}
