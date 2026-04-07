package lsj.qg.finaltrain.controller;

import lsj.qg.finaltrain.pojo.User;
import lsj.qg.finaltrain.service.impl.UserServiceImpl;
import lsj.qg.finaltrain.utils.JwtUtil;
import lsj.qg.finaltrain.utils.ResultJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/Users")
@CrossOrigin(origins = "*") //开启跨域
public class UserController {

    @Autowired
    private UserServiceImpl userServiceImpl;

    @Autowired
    private JwtUtil jwtUtil;

    //注册
    @PostMapping("/register")
    public ResultJson<String> registerUser(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String confirmPassword = request.get("confirmPassword");
            String email = request.get("email");
            String phone = request.get("phone");

            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setEmail(email);
            user.setPhone(phone);

            userServiceImpl.register(user, confirmPassword);
            return ResultJson.success("注册成功");
        } catch (IllegalArgumentException e) {
            return ResultJson.error(e.getMessage());
        } catch (Exception e) {
            return ResultJson.error("注册失败");
        }
    }

    //登录
    @PostMapping("/login")
    public ResultJson<Map<String, Object>> loginUser(@RequestBody Map<String, String> request) {
        try {
            String account = request.get("account");
            String password = request.get("password");

            User user = userServiceImpl.login(account, password);

            // 生成token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userid", user.getId().toString());
            claims.put("username", user.getUsername());
            claims.put("status", user.getStatus());
            String token = JwtUtil.createToken(claims);

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", user);

            return ResultJson.success("登录成功", data);
        } catch (IllegalArgumentException e) {
            return ResultJson.error(e.getMessage());
        } catch (Exception e) {
            return ResultJson.error("登录失败");
        }
    }
}
