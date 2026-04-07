package lsj.qg.finaltrain.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {
    private String signature = "mysupersecretkeyforjwttokengenerationmustbelongenough123456";

    public static String createToken(Map<String,Object> map) {
        return JWT.create()
                        .withClaim("claims", map)//添加载荷
                        .withExpiresAt(new Date(System.currentTimeMillis()+1000*60*60*12))//添过期时间
                        .sign(Algorithm.HMAC256("QGQGQG"));//指定算法,配置秘钥
    }

    public static Map<String,Object> verifyToken(String token) {

        return JWT
                .require(Algorithm.HMAC256("QGQGQG"))
                .build()//构建解析器
                .verify(token)
                .getClaim("claims")
                .asMap();

    }
}
