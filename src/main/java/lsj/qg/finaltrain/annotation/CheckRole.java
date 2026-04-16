package lsj.qg.finaltrain.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


// 定义一个注解，用来标记需要检查角色权限的方法
@Target(ElementType.METHOD) // 表示这个注解用在方法上
@Retention(RetentionPolicy.RUNTIME) // 表示运行时通过反射能读到
public @interface CheckRole {
    // 定义一个属性，用来接收允许访问的角色名称
    // 比如：ADMIN 或者 USER
    int value();
}
