package lsj.qg.finaltrain.service;

/**
 * 验证码存储服务接口
 * 用于存储和验证邮箱验证码
 */
public interface VerifiCodeService {
    
    /**
     * 保存验证码
     * @param email 邮箱地址
     * @param code 验证码
     * @param type 验证码类型（register/login/reset）
     */
    void saveCode(String email, String code, String type);
    
    /**
     * 验证验证码
     * @param email 邮箱地址
     * @param code 用户输入的验证码
     * @param type 验证码类型
     * @return 验证是否通过
     */
    boolean verifyCode(String email, String code, String type);
    
    /**
     * 删除验证码
     * @param email 邮箱地址
     * @param type 验证码类型
     */
    void deleteCode(String email, String type);
    
    /**
     * 检查是否可以发送验证码（防刷）
     * @param email 邮箱地址
     * @return 是否可以发送
     */
    boolean canSendCode(String email);
}
