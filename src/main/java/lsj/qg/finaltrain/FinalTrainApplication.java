package lsj.qg.finaltrain;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

//扫描mapper
@MapperScan("lsj.qg.finaltrain.mapper")
// 在注解里加上 exclude 排除掉安全自动配置，否则和自己写的拦截器冲突
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
public class FinalTrainApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =SpringApplication.run(FinalTrainApplication.class, args);
    }

}