package lsj.qg.finaltrain.config;

import lsj.qg.finaltrain.intercepters.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                //设置拦截路径
                .addPathPatterns("/StuItems/**", "/Users/**", "/message/**")
                //设置拦截白名单
                .excludePathPatterns("/Users/login", "/Users/register");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许前端跨域访问（尤其是携带 Authorization header 时会触发 OPTIONS 预检）
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 定义你的固定物理路径
        String filePath = "D:\\Java\\FinalTrain\\msg\\";

        // 注册映射规则
        // 含义：当浏览器访问 http://localhost:8080/msg/xxx.jpg 时
        // 服务器会去读取上面 filePath 目录下的 xxx.jpg 文件并拼接路径
        registry.addResourceHandler("/msg/**")
                .addResourceLocations("file:" + filePath);
    }
}
