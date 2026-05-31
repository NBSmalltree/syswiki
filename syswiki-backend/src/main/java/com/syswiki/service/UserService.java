package com.syswiki.service;

import com.syswiki.model.dto.LoginDTO;
import com.syswiki.model.dto.RegisterDTO;
import com.syswiki.model.entity.SysUser;
import com.syswiki.model.vo.TokenVO;
import com.syswiki.model.vo.UserVO;
import java.util.List;

public interface UserService {
    TokenVO login(LoginDTO dto, String ip);
    TokenVO register(RegisterDTO dto);
    List<UserVO> listUsers();
    UserVO getUser(String userId);
    void updateRole(String userId, String role);
    void disableUser(String userId);
    void changePassword(String userId, String oldPassword, String newPassword);
    void resetPassword(String userId, String newPassword);
}
