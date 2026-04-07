package lsj.qg.finaltrain.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lsj.qg.finaltrain.mapper.UserMapper;
import lsj.qg.finaltrain.pojo.User;
import lsj.qg.finaltrain.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtil jwtutil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User findByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        return userMapper.selectOne(queryWrapper);
    }

    //如果报错回滚事务
    @Transactional
    public boolean register(User user, String confirmPassword) {
        // 校验密码长度
        if (user.getPassword().length() < 6 || user.getPassword().length() > 20) {
            throw new IllegalArgumentException("密码长度必须在6-20位之间");
        }
        // 校验两次密码一致
        if (!user.getPassword().equals(confirmPassword)) {
            throw new IllegalArgumentException("两次密码输入不一致");
        }
        // 检查用户名是否已存在
        if (findByUsername(user.getUsername()) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        //如果邮箱格式不正确
        if (!user.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("邮箱格式有误");
        }
        //如果手机号不正确
        if (!user.getPhone().matches("^1[3-9]\\d{9}$")) {
            throw new IllegalArgumentException("手机号格式有误");
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 设置默认值
        user.setRole(0); // 用户
        user.setStatus(0); // 正常
        user.setLastLoginTime(LocalDateTime.now());
        return userMapper.insert(user) > 0;
    }

    //登录
    public User login(String account, String password) {
        // 查询条件：(phone = account OR email = account)
        User user = userMapper.selectOne(new QueryWrapper<User>()
                .eq("phone", account)
                .or()
                .eq("email", account));

        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }

        // 使用你引入的 BCrypt 校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        return user;
    }
}
