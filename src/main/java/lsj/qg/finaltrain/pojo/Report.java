package lsj.qg.finaltrain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reporterId;

    private Long postId;

    private String reason;

    private Integer status; // 0-待处理, 1-已处理
}
