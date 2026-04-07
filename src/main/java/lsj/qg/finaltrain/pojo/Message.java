package lsj.qg.finaltrain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer type; // 1-评论, 2-私聊

    private Long postId; // 如果是私聊，前端传null即可

    private Long senderId;

    private Long receiverId;

    private String content;

    private Integer isRead; // 0-未读, 1-已读

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
