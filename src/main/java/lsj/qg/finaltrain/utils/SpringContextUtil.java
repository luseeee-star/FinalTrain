package lsj.qg.finaltrain.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringContextUtil implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }
}
/*  这个类的作用
    * 1、当 Spring 程序启动时，它会扫描到标记了 @Component 的 SpringContextUtils。
    * 因为该类实现了 ApplicationContextAware 接口，Spring 会自动调用 setApplicationContext 方法，
    * 把整个 Spring 的（ApplicationContext）塞给它。
    *
    * 2、以后任何地方（哪怕是不受 Spring 管理的类）想要 Spring 里的东西，
    * 只需要调用 SpringContextUtils.getBean(UserMapper.class)。
    * 这个静态方法会找那个存好的管理类中
* */