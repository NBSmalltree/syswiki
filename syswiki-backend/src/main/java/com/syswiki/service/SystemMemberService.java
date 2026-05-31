package com.syswiki.service;

import com.syswiki.model.vo.UserVO;
import java.util.List;

public interface SystemMemberService {
    List<UserVO> listMembers(String systemId);
    void addMember(String systemId, String userId, String role);
    void removeMember(String systemId, String userId);
}
