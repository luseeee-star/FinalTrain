package lsj.qg.finaltrain.controller;

import jakarta.servlet.http.HttpSession;
import lsj.qg.finaltrain.annotation.CheckRole;
import lsj.qg.finaltrain.mapper.UserMapper;
import lsj.qg.finaltrain.pojo.User;
import lsj.qg.finaltrain.service.UserService;
import lsj.qg.finaltrain.service.VerifiCodeService;
import lsj.qg.finaltrain.utils.EmailUtil;
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
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private EmailUtil emailUtil;
    
    @Autowired
    private VerifiCodeService verifiCodeService;

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

            userService.register(user, confirmPassword);
            return ResultJson.success("注册成功");
        } catch (IllegalArgumentException e) {
            return ResultJson.error(e.getMessage());
        } catch (Exception e) {
            return ResultJson.systemError();
        }
    }
    
    //发送注册验证码
    @PostMapping("/sendRegisterCode")
    public ResultJson<String> sendRegisterCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            
            if (email == null || !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                return ResultJson.error("邮箱格式有误");
            }
            
            // 检查邮箱是否已注册
            if (userService.findByEmail(email) != null) {
                return ResultJson.error("该邮箱已被注册");
            }
            
            // 检查发送频率
            if (!verifiCodeService.canSendCode(email)) {
                return ResultJson.error("发送过于频繁，请60秒后重试");
            }
            
            String code = emailUtil.generateCode();
            emailUtil.sendVerificationCode(email, code, "register");
            verifiCodeService.saveCode(email, code, "register");
            
            return ResultJson.success("验证码已发送");
        } catch (Exception e) {
            return ResultJson.systemError();
        }
    }
    
    //验证码注册
    @PostMapping("/registerWithCode")
    public ResultJson<String> registerWithCode(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String confirmPassword = request.get("confirmPassword");
            String email = request.get("email");
            String phone = request.get("phone");
            String code = request.get("code");

            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setEmail(email);
            user.setPhone(phone);

            userService.registerWithCode(user, confirmPassword, code);
            return ResultJson.success("注册成功");
        } catch (IllegalArgumentException e) {
            return ResultJson.error(e.getMessage());
        } catch (Exception e) {
            return ResultJson.systemError();
        }
    }

    //登录
    @PostMapping("/login")
    public ResultJson<Map<String, Object>> loginUser(@RequestBody Map<String, String> request, HttpSession session) {
        try {
            String account = request.get("account");
            String password = request.get("password");

            User user = userService.login(account, password);

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
            return ResultJson.systemError();
        }
    }
    
    //发送登录验证码
    @PostMapping("/sendLoginCode")
    public ResultJson<String> sendLoginCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            
            if (email == null || !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                return ResultJson.error("邮箱格式有误");
            }
            
            // 检查邮箱是否已注册
            if (userService.findByEmail(email) == null) {
                return ResultJson.error("该邮箱未注册");
            }
            
            // 检查发送频率
            if (!verifiCodeService.canSendCode(email)) {
                return ResultJson.error("发送过于频繁，请60秒后重试");
            }
            
            String code = emailUtil.generateCode();
            emailUtil.sendVerificationCode(email, code, "login");
            verifiCodeService.saveCode(email, code, "login");
            
            return ResultJson.success("验证码已发送");
        } catch (Exception e) {
            return ResultJson.systemError();
        }
    }
    
    //验证码登录
    @PostMapping("/loginWithCode")
    public ResultJson<Map<String, Object>> loginWithCode(@RequestBody Map<String, String> request, HttpSession session) {
        try {
            String email = request.get("email");
            String code = request.get("code");

            User user = userService.loginWithCode(email, code);

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
            return ResultJson.systemError();
        }
    }
    
    //发送密码重置验证码
    @PostMapping("/sendResetCode")
    public ResultJson<String> sendResetCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            
            if (email == null || !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                return ResultJson.error("邮箱格式有误");
            }
            
            // 检查邮箱是否已注册
            if (userService.findByEmail(email) == null) {
                return ResultJson.error("该邮箱未注册");
            }
            
            // 检查发送频率
            if (!verifiCodeService.canSendCode(email)) {
                return ResultJson.error("发送过于频繁，请60秒后重试");
            }
            
            String code = emailUtil.generateCode();
            emailUtil.sendVerificationCode(email, code, "reset");
            verifiCodeService.saveCode(email, code, "reset");
            
            return ResultJson.success("验证码已发送");
        } catch (Exception e) {
            return ResultJson.systemError();
        }
    }
    
    //重置密码
    @PostMapping("/resetPassword")
    public ResultJson<String> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String newPassword = request.get("newPassword");
            String confirmPassword = request.get("confirmPassword");
            String code = request.get("code");

            userService.resetPassword(email, newPassword, confirmPassword, code);
            return ResultJson.success("密码重置成功");
        } catch (IllegalArgumentException e) {
            return ResultJson.error(e.getMessage());
        } catch (Exception e) {
            return ResultJson.systemError();
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
            return ResultJson.systemError();
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
            return ResultJson.systemError();
        }
    }
}
