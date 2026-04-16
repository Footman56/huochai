package com.huochai.auth.infrastructure.security;

import cn.hutool.core.collection.CollUtil;
import com.huochai.auth.domain.model.AuthUser;
import com.huochai.auth.domain.repository.AuthUserRepository;
import com.huochai.permission.domain.service.RbacDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户详情服务实现
 *
 * @author huochai
 */
@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private RbacDomainService rbacDomainService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 查询用户
        AuthUser user = authUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        // 加载权限
        List<String> permissions = rbacDomainService.loadUserPermissions(user.getId());
        
        // 加载角色
        List<String> roles = rbacDomainService.getUserRoleCodes(user.getId());

        // 构建登录用户
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setStatus(user.getStatus());
        loginUser.setPermissions(CollUtil.isEmpty(permissions) ? List.of() : permissions);
        loginUser.setRoles(CollUtil.isEmpty(roles) ? List.of() : roles);

        log.debug("加载用户详情: username={}, permissions={}", username, permissions);

        return loginUser;
    }

    /**
     * 根据用户ID加载用户详情
     */
    public UserDetails loadUserByUserId(Long userId) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + userId));

        return loadUserByUsername(user.getUsername());
    }
}