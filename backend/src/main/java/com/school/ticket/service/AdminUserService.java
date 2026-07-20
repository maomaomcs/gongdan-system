package com.school.ticket.service;

import com.school.ticket.dto.AdminUserResponse;
import com.school.ticket.dto.CreateAdminRequest;
import com.school.ticket.entity.AdminUser;
import com.school.ticket.repository.AdminUserRepository;
import com.school.ticket.web.ApiException;
import com.school.ticket.web.TokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserRepository repo;
    private final PasswordEncoder encoder;
    private final TokenStore tokenStore;

    /** 校验用户名密码,成功返回用户,失败抛异常 */
    @Transactional(readOnly = true)
    public AdminUser authenticate(String username, String password) {
        AdminUser u = repo.findByUsername(username).orElse(null);
        if (u == null || !Boolean.TRUE.equals(u.getEnabled()) || !encoder.matches(password, u.getPasswordHash())) {
            throw new ApiException(401, "用户名或密码错误");
        }
        return u;
    }

    @Transactional(readOnly = true)
    public AdminUser getByUsername(String username) {
        return repo.findByUsername(username)
                .orElseThrow(() -> new ApiException(401, "账号不存在"));
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list() {
        return repo.findAll().stream()
                .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse create(CreateAdminRequest req) {
        String username = req.username().trim();
        if (repo.existsByUsername(username)) {
            throw new ApiException(400, "用户名已存在");
        }
        AdminUser u = new AdminUser();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setDisplayName(StringUtils.hasText(req.displayName()) ? req.displayName().trim() : username);
        u.setEnabled(true);
        repo.save(u);
        return AdminUserResponse.from(u);
    }

    @Transactional
    public void delete(Long id, String currentUsername) {
        AdminUser u = repo.findById(id)
                .orElseThrow(() -> new ApiException(404, "账号不存在"));
        if (u.getUsername().equals(currentUsername)) {
            throw new ApiException(400, "不能删除当前登录的账号");
        }
        if (repo.count() <= 1) {
            throw new ApiException(400, "至少保留一个管理员账号");
        }
        tokenStore.revokeUser(u.getUsername(), TokenStore.ROLE_ADMIN);
        repo.delete(u);
    }

    @Transactional
    public void setEnabled(Long id, boolean enabled, String currentUsername) {
        AdminUser u = repo.findById(id)
                .orElseThrow(() -> new ApiException(404, "账号不存在"));
        if (!enabled && u.getUsername().equals(currentUsername)) {
            throw new ApiException(400, "不能停用当前登录的账号");
        }
        u.setEnabled(enabled);
        repo.save(u);
        if (!enabled) tokenStore.revokeUser(u.getUsername(), TokenStore.ROLE_ADMIN);
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        AdminUser u = getByUsername(username);
        if (!encoder.matches(oldPassword, u.getPasswordHash())) {
            throw new ApiException(400, "原密码错误");
        }
        u.setPasswordHash(encoder.encode(newPassword));
        repo.save(u);
        tokenStore.revokeUser(username, TokenStore.ROLE_ADMIN); // 改密后需重新登录
    }

    /** 首次启动:库中无账号时创建初始管理员 */
    @Transactional
    public void seedIfEmpty(String username, String rawPassword) {
        if (repo.count() > 0) return;
        AdminUser u = new AdminUser();
        u.setUsername(username);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setDisplayName(username);
        u.setEnabled(true);
        repo.save(u);
    }
}
