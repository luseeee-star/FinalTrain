package lsj.qg.finaltrain.intercepters;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lsj.qg.finaltrain.utils.JwtUtil;
import lsj.qg.finaltrain.utils.ThreadLocalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LoginInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler){
        // 允许跨域预检请求：浏览器在携带 Authorization 等自定义头时会先发送 OPTIONS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("处理跨域预检请求 (OPTIONS)");
            return true;
        }
        String token = request.getHeader("Authorization");
        try {
            Map<String,Object> claims = JwtUtil.verifyToken(token);
            String userid = (String) claims.get("userid");
            Integer role = (Integer) claims.get("status");
            log.info("用户 {} (角色: {}) 认证成功, 访问路径: {}", userid, role == 0 ? "学生" : "维修人员", request.getRequestURI());
            //把业务数据存放到线程里
            ThreadLocalUtil.set(claims);
            return  true;
        } catch (Exception e) {
            log.warn("令牌验证失败 - 原因: {}, 访问路径: {}", e.getMessage(), request.getRequestURI());
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        if (ex != null) {
            log.error("请求处理异常 - 路径: {}, 异常: {}", request.getRequestURI(), ex.getMessage(), ex);
        }
        ThreadLocalUtil.remove();
    }
}
