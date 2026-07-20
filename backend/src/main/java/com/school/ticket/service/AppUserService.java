package com.school.ticket.service;

import com.school.ticket.dto.UserAuthRequest;
import com.school.ticket.entity.AppUser;
import com.school.ticket.repository.AppUserRepository;
import com.school.ticket.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository repo;
    private final PasswordEncoder encoder;

    @Transactional
    public AppUser register(UserAuthRequest req) {
        String username = req.username().trim();
        if (repo.existsByUsername(username)) {
            throw new ApiException(400, "该用户名已被注册");
        }
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setDisplayName(StringUtils.hasText(req.displayName()) ? req.displayName().trim() : username);
        u.setPhone(StringUtils.hasText(req.phone()) ? req.phone().trim() : null);
        u.setEnabled(true);
        return repo.save(u);
    }

    @Transactional(readOnly = true)
    public AppUser authenticate(String username, String password) {
        AppUser u = repo.findByUsername(username.trim()).orElse(null);
        if (u == null || !Boolean.TRUE.equals(u.getEnabled()) || !encoder.matches(password, u.getPasswordHash())) {
            throw new ApiException(401, "用户名或密码错误");
        }
        return u;
    }

    @Transactional(readOnly = true)
    public AppUser getByUsername(String username) {
        return repo.findByUsername(username)
                .orElseThrow(() -> new ApiException(401, "账号不存在"));
    }
}
