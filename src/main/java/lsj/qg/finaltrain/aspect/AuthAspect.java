package lsj.qg.finaltrain.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lsj.qg.finaltrain.annotation.CheckRole;
import lsj.qg.finaltrain.utils.JwtUtil;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;


@Aspect
@Component
public class AuthAspect {
    // 1. 定义“拦截规则”：拦截所有标有 @CheckRole 注解的方法
    @Pointcut("@annotation(lsj.qg.finaltrain.annotation.CheckRole)")
    public void checkRolePoint() {}

    // 注入注解对象
    @Before("@annotation(checkRole)")
    public void doCheck(CheckRole checkRole) {
        // 1. 获取请求
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 2. 获取 Token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("请先登录");
        }

        // 3. 解析 Token 获取当前用户的角色
        int currentRole;
        try {
            Map<String, Object> claims = JwtUtil.verifyToken(token);
            currentRole = (Integer) claims.get("role");
        } catch (Exception e) {
            throw new RuntimeException("登录已过期或 Token 无效");
        }

        // 4. 获取注解上要求的角色
        int requiredRole = checkRole.value();

        // 如果是 -1 (默认值)，说明不需要校验特定角色，只要登录就行
        if (requiredRole == -1) {
            return;
        }

        // 5. 核心比对：整数比对
        if (currentRole != requiredRole) {
            // 这里可以加一个简单的逻辑来显示中文角色名，方便调试
            String currentRoleName = (currentRole == 0) ? "学生" : "管理员";
            String requiredRoleName = (requiredRole == 0) ? "学生" : "管理员";

            throw new RuntimeException("权限不足！你是[" + currentRoleName + "]，需要[" + requiredRoleName + "]权限");
        }
    }
}
