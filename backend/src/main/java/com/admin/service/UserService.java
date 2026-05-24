package com.admin.service;

import com.admin.dto.*;
import com.admin.entity.User;
import com.admin.entity.Role;
import com.admin.mapper.RoleMapper;
import com.admin.exception.BusinessException;
import com.admin.mapper.UserMapper;
import com.admin.mapper.UserRoleMapper;
import com.admin.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        
        if (user.getStatus() != 1) {
            throw new BusinessException("用户已被禁用");
        }
        
        String token = jwtUtil.generateToken(user.getUsername());
        user.setPassword(null); // 不返回密码
        
        return new LoginResponse(token, user);
    }

    public User getUserByUsername(String username) {
        User user = userMapper.findByUsername(username);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    public User getUserById(Long id) {
        User user = userMapper.findById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    public List<User> getAllUsers() {
        List<User> users = userMapper.findAll();
        users.forEach(this::fillUserExtra);
        return users;
    }

    public PageResult<User> getUserPage(UserQueryRequest query) {
        long total = userMapper.countByCondition(query.getUsername(), query.getStatus());
        List<User> records = userMapper.findByPage(
                query.getUsername(), query.getStatus(),
                query.getOffset(), query.getPageSize());
        records.forEach(this::fillUserExtra);
        return PageResult.of(records, total, query);
    }

    private void fillUserExtra(User user) {
        user.setPassword(null);
        List<Role> roles = roleMapper.findByUserId(user.getId());
        user.setRoleNames(roles.stream().map(Role::getRoleName).toList());
    }

    public void register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (userMapper.findByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(1);
        userMapper.insert(user);
    }

    public int createUser(UserCreateRequest request) {
        if (userMapper.findByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAvatar(request.getAvatar());
        user.setStatus(request.getStatus());
        return userMapper.insert(user);
    }

    public int updateUser(UserUpdateRequest request) {
        User user = new User();
        user.setId(request.getId());
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAvatar(request.getAvatar());
        user.setStatus(request.getStatus());
        return userMapper.update(user);
    }

    public User updateProfile(String username, ProfileUpdateRequest request) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAvatar(request.getAvatar());
        userMapper.updateProfile(user);
        user.setPassword(null);
        return user;
    }

    public int deleteUser(Long id) {
        return userMapper.deleteById(id);
    }

    public void resetPassword(Long userId, String newPassword) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.updatePassword(user.getUsername(), passwordEncoder.encode(newPassword));
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        userMapper.updatePassword(username, passwordEncoder.encode(newPassword));
    }

    public List<Long> getUserRoleIds(Long userId) {
        return userRoleMapper.findRoleIdsByUserId(userId);
    }

    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                userRoleMapper.insert(userId, roleId);
            }
        }
    }
}
