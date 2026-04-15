package lsj.qg.finaltrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lsj.qg.finaltrain.mapper.UserMapper;
import lsj.qg.finaltrain.pojo.User;
import lsj.qg.finaltrain.service.UserService;
import lsj.qg.finaltrain.service.VerifiCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private VerifiCodeService verifiCodeService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public User findByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
    
    @Override
    public User findByEmail(String email) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
    }

    //如果报错回滚事务
    @Transactional
    @Override
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
    
    //如果报错回滚事务
    @Transactional
    @Override
    public boolean registerWithCode(User user, String confirmPassword, String code) {
        // 校验验证码
        if (!verifiCodeService.verifyCode(user.getEmail(), code, "register")) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        
        // 调用原有注册逻辑
        return register(user, confirmPassword);
    }

    //登录
    @Override
    public User login(String account, String password) {
        // mp框架用eq条件查询，(手机号或邮箱) + 密码
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, account)
                .or()
                .eq(User::getEmail, account));

        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }

        if (Integer.valueOf(1).equals(user.getStatus())) {
            throw new IllegalArgumentException("您已被封禁");
        }

        // 使用BCrypt校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        //更新的话要么新创建user对象把要更新的东西传入user中，要么就用null然后更新内容和查询条件放在第二个
        userMapper.update(null,new LambdaUpdateWrapper<User>()
                    .set(User::getLastLoginTime, LocalDateTime.now())
                    .eq(User::getId, user.getId()));
        return user;
    }
    
    @Override
    public User loginWithCode(String email, String code) {
        // 校验验证码
        if (!verifiCodeService.verifyCode(email, code, "login")) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        
        // 查找用户
        User user = findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("该邮箱未注册");
        }
        
        if (Integer.valueOf(1).equals(user.getStatus())) {
            throw new IllegalArgumentException("您已被封禁");
        }
        
        // 更新最后登录时间
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .set(User::getLastLoginTime, LocalDateTime.now())
                .eq(User::getId, user.getId()));
        
        return user;
    }
    
    //如果报错回滚事务
    @Transactional
    @Override
    public boolean resetPassword(String email, String newPassword, String confirmPassword, String code) {
        // 校验验证码
        if (!verifiCodeService.verifyCode(email, code, "reset")) {
            throw new IllegalArgumentException("验证码错误或已过期");
        }
        
        // 校验密码长度
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw new IllegalArgumentException("密码长度必须在6-20位之间");
        }
        
        // 校验两次密码一致
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次密码输入不一致");
        }
        
        // 查找用户
        User user = findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("该邮箱未注册");
        }
        
        // 更新密码
        User update = new User();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(newPassword));
        
        return userMapper.updateById(update) > 0;
    }
}
