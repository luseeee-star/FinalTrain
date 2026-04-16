package lsj.qg.finaltrain.mapper;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("claim_request")
public class ClaimRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的物品发布ID (ItemPost表的ID)
     */
    private Long itemPostId;

    /**
     * 发起认领申请的用户ID
     */
    private Long applicant_id;

    /**
     * 物品发布者的用户ID
     */
    private Long owner_id;

    /**
     * 用户填写的核验信息 (比如：物品的某个只有失主知道的特征) [cite: 144]
     */
    private String verificationAnswer;

    /**
     * 申请状态
     * 0: 待审核 (PENDING)
     * 1: 已同意 (ACCEPTED)
     * 2: 已拒绝 (REJECTED)
     * 3: 要求补充证据 (NEED_MORE_INFO)
     */
    private Integer status;

    /**
     * 审批反馈信息（拒绝理由或补充建议）
     */
    private String rejectionReason;

    /**
     * 一次性取件码/暗号
     */
    private String pickupCode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 以下为业务字段，不属于数据库表结构
    @TableField(exist = false)
    private String applicantNickname;

    @TableField(exist = false)
    private String itemName;
}
