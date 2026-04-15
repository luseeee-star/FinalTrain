package lsj.qg.finaltrain.service.impl;

import lsj.qg.finaltrain.service.VerifiCodeService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerifiCodeServiceImpl implements VerifiCodeService {
    
    // 使用内存存储验证码，格式: email:type -> CodeInfo
    private final Map<String, CodeInfo> codeStorage = new ConcurrentHashMap<>();
    
    // 发送时间记录，用于防刷，格式: email -> lastSendTime
    private final Map<String, Long> sendTimeRecord = new ConcurrentHashMap<>();
    
    // 验证码有效期：5分钟（毫秒）
    private static final long CODE_EXPIRE_TIME = 5 * 60 * 1000;
    
    // 发送间隔：60秒（毫秒）
    private static final long SEND_INTERVAL = 60 * 1000;


    //为了安全性和专业规范，定义一个内部类来存储验证码信息
    private static class CodeInfo {
        String code;
        long expireTime;
        
        CodeInfo(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
        }
    }
    
    @Override
    public void saveCode(String email, String code, String type) {
        String key = buildKey(email, type);
        codeStorage.put(key, new CodeInfo(code, System.currentTimeMillis() + CODE_EXPIRE_TIME));
        sendTimeRecord.put(email, System.currentTimeMillis());
    }
    
    @Override
    public boolean verifyCode(String email, String code, String type) {
        String key = buildKey(email, type);
        CodeInfo codeInfo = codeStorage.get(key);
        
        if (codeInfo == null) {
            return false;
        }
        
        // 检查是否过期
        if (System.currentTimeMillis() > codeInfo.expireTime) {
            codeStorage.remove(key);
            return false;
        }
        
        // 验证成功后删除验证码
        if (codeInfo.code.equals(code)) {
            codeStorage.remove(key);
            return true;
        }
        
        return false;
    }
    
    @Override
    public void deleteCode(String email, String type) {
        String key = buildKey(email, type);
        codeStorage.remove(key);
    }
    
    @Override
    public boolean canSendCode(String email) {
        Long lastSendTime = sendTimeRecord.get(email);
        if (lastSendTime == null) {
            return true;
        }
        return System.currentTimeMillis() - lastSendTime >= SEND_INTERVAL;
    }
    
    private String buildKey(String email, String type) {
        return email + ":" + type;
    }
}
