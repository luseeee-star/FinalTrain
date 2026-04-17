package lsj.qg.finaltrain.pojo;

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
    @TableField("applicant_id")
    private Long applicantId;

    /**
     * 物品发布者的用户ID
     */
    @TableField("owner_id")
    private Long ownerId;

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


    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
