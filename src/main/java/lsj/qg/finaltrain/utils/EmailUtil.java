package lsj.qg.finaltrain.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class EmailUtil {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    //生成6位数字验证码
    public String generateCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 发送验证码邮件
     * @param toEmail 收件人邮箱
     * @param code 验证码
     * @param type 邮件类型（注册/登录/找回密码）
     */
    public void sendVerificationCode(String toEmail, String code, String type) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        
        String subject;
        String content;

        switch (type) {
            case "register":
                subject = "【失物招领平台】注册验证码";
                content = "您好！\n\n您的注册验证码是：" + code + "\n\n验证码5分钟内有效，请勿泄露给他人。\n\n如非本人操作，请忽略此邮件。";
                break;
            case "login":
                subject = "【失物招领平台】登录验证码";
                content = "您好！\n\n您的登录验证码是：" + code + "\n\n验证码5分钟内有效，请勿泄露给他人。\n\n如非本人操作，请立即修改密码。";
                break;
            case "reset":
                subject = "【失物招领平台】密码重置验证码";
                content = "您好！\n\n您的密码重置验证码是：" + code + "\n\n验证码5分钟内有效，请勿泄露给他人。\n\n如非本人操作，请立即修改密码。";
                break;
            default:
                subject = "【失物招领平台】验证码";
                content = "您好！\n\n您的验证码是：" + code + "\n\n验证码5分钟内有效。";
        }

        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
}
