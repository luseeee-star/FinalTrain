package lsj.qg.finaltrain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@TableName("item_post")
public class ItemPost {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId; // 发布者ID

    private Integer type; // 1-丢失, 2-拾取

    private String itemName;

    private String location;

    private LocalDateTime eventTime; // 丢失/拾取的时间

    private String description;

    private String aiDescription; // Spring AI 生成的描述存这里

    private String imageUrl;

    private String contactInfo;

    private Integer status; // 0-寻找中, 1-已完成

    private Integer isPinned; // 0-普通, 1-置顶

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
