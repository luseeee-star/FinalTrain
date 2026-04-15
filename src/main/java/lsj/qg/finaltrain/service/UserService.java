package lsj.qg.finaltrain.service;

import lsj.qg.finaltrain.pojo.User;

public interface UserService {
        User findByUsername(String username);
        
        User findByEmail(String email);

        boolean register(User user, String confirmPassword);
        
        boolean registerWithCode(User user, String confirmPassword, String code);

        User login(String account, String password);
        
        User loginWithCode(String email, String code);
        
        boolean resetPassword(String email, String newPassword, String confirmPassword, String code);
}
