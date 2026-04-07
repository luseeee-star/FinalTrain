package lsj.qg.finaltrain.service;

import lsj.qg.finaltrain.pojo.User;

public interface UserService {
        User findByUsername(String username);

        boolean register(User user, String confirmPassword);

        User login(String account, String password);
}
