package lsj.qg.finaltrain.controller;

import jakarta.servlet.http.HttpSession;
import lsj.qg.finaltrain.mapper.UserMapper;
import lsj.qg.finaltrain.pojo.User;
import lsj.qg.finaltrain.service.impl.UserServiceImpl;
import lsj.qg.finaltrain.utils.JwtUtil;
import lsj.qg.finaltrain.utils.ResultJson;
import lsj.qg.finaltrain.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/Users")
@CrossOrigin(origins = "*") //开启跨域
public class UserController {
    private static final String AVATAR_DIR = "D:\\Java\\FinalTrain\\msg\\";

    @Autowired
    private UserMapper userMapper;

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
    public ResultJson<Map<String, Object>> loginUser(@RequestBody Map<String, String> request, HttpSession session) {
        try {
            String account = request.get("account");
            String password = request.get("password");

            User user = userServiceImpl.login(account, password);

            // 生成token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userid", user.getId().toString());
            claims.put("username", user.getUsername());
            claims.put("status", user.getStatus());
            claims.put("role", user.getRole());
            String token = JwtUtil.createToken(claims);

            //生成session数据
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("nickname", user.getNickname());

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

    @GetMapping("/profile")
    public ResultJson<Map<String, Object>> getProfile() {
        try {
            Map<String, String> claims = ThreadLocalUtil.get();
            Long userId = Long.parseLong(claims.get("userid"));
            User user = userMapper.selectById(userId);
            if (user == null) {
                return ResultJson.error("用户不存在");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("nickname", user.getNickname());
            data.put("avatarUrl", user.getAvatarUrl());
            data.put("email", user.getEmail());
            data.put("phone", user.getPhone());
            return ResultJson.success("查询成功", data);
        } catch (Exception e) {
            return ResultJson.error("查询失败");
        }
    }

    @PostMapping("/profile")
    public ResultJson<String> updateProfile(@RequestParam(value = "nickname", required = false) String nickname,
                                            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            Map<String, String> claims = ThreadLocalUtil.get();
            Long userId = Long.parseLong(claims.get("userid"));

            User update = new User();
            update.setId(userId);

            if (nickname != null) {
                String nick = nickname.trim();
                if (!nick.isEmpty()) {
                    update.setNickname(nick);
                }
            }

            if (file != null && !file.isEmpty()) {
                String originalName = file.getOriginalFilename();
                String ext = ".jpg";
                if (originalName != null && originalName.lastIndexOf('.') >= 0) {
                    ext = originalName.substring(originalName.lastIndexOf('.'));
                }
                String fileName = UUID.randomUUID() + ext;
                File dir = new File(AVATAR_DIR);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                file.transferTo(new File(AVATAR_DIR + fileName));
                update.setAvatarUrl("/msg/" + fileName);
            }

            if (update.getNickname() == null && update.getAvatarUrl() == null) {
                return ResultJson.error("请至少修改一个字段");
            }

            userMapper.updateById(update);
            return ResultJson.success("更新成功");
        } catch (Exception e) {
            return ResultJson.error("更新失败");
        }
    }
}
