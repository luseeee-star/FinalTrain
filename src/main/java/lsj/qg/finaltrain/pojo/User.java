package lsj.qg.finaltrain.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password; // 记住：存储时要加密

    private String nickname; // 展示名称

    private String email;

    private String phone;

    private Integer role; // 0-用户, 1-管理

    private Integer status; // 0-正常, 1-封禁

    @TableField(fill = FieldFill.INSERT) // 自动填充
    private LocalDateTime lastLoginTime;
}
