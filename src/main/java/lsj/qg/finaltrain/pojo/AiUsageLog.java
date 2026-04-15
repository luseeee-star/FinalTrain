package lsj.qg.finaltrain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_usage_log") // 告诉 MyBatis-Plus 这个类对应哪张表
public class AiUsageLog {

    @TableId(type = IdType.AUTO) // ID 自增
    private Long id;

    @TableField("user_id") //（驼峰 vs 下划线），需要指定
    private Long userId;

    @TableField("create_time")
    private LocalDateTime createTime;
}
