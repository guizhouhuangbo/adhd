package com.huangbo.adhd.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huangbo.adhd.dto.LoginRequest;
import com.huangbo.adhd.dto.LoginResponse;
import com.huangbo.adhd.dto.UserProfileResponse;
import com.huangbo.adhd.entity.User;
import com.huangbo.adhd.integration.WeChatAuthClient;
import com.huangbo.adhd.mapper.UserMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final WeChatAuthClient weChatAuthClient;

    public AuthService(UserMapper userMapper, WeChatAuthClient weChatAuthClient) {
        this.userMapper = userMapper;
        this.weChatAuthClient = weChatAuthClient;
    }

    public LoginResponse login(LoginRequest request) {
        String openId = weChatAuthClient.exchangeOpenId(request.code());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenId, openId));
        if (user == null) {
            user = new User();
            user.setOpenId(openId);
            user.setNickname(request.nickname() == null || request.nickname().isBlank() ? "新来的家长" : request.nickname());
            user.setAvatarUrl(request.avatarUrl());
            user.setChildName("小星");
            user.setChildAge(8);
            user.setTotalStars(0);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
        } else {
            if (request.nickname() != null && !request.nickname().isBlank()) {
                user.setNickname(request.nickname());
            }
            if (request.avatarUrl() != null && !request.avatarUrl().isBlank()) {
                user.setAvatarUrl(request.avatarUrl());
            }
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        return new LoginResponse(token, toProfile(user));
    }

    public UserProfileResponse getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalStateException("用户不存在");
        }
        return toProfile(user);
    }

    private UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(
            user.getId(),
            user.getNickname(),
            user.getAvatarUrl(),
            user.getChildName(),
            user.getChildAge(),
            user.getTotalStars()
        );
    }
}
