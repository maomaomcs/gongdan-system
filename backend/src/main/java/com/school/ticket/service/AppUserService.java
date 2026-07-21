package com.school.ticket.service;

import com.school.ticket.dto.AppUserResponse;
import com.school.ticket.dto.PageResponse;
import com.school.ticket.dto.UserAuthRequest;
import com.school.ticket.entity.AdminUser;
import com.school.ticket.entity.AppUser;
import com.school.ticket.repository.AdminUserRepository;
import com.school.ticket.repository.AppUserRepository;
import com.school.ticket.web.ApiException;
import com.school.ticket.web.TokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository repo;
    private final AdminUserRepository adminRepo;
    private final InviteCodeService inviteCodeService;
    private final PasswordEncoder encoder;
    private final TokenStore tokenStore;

    @Transactional
    public AppUser register(UserAuthRequest req) {
        String username = req.username().trim();
        if (repo.existsByUsername(username)) {
            throw new ApiException(400, "该用户名已被注册");
        }
        // 校验并消耗邀请码(校验码)
        inviteCodeService.validateAndConsume(req.inviteCode());
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
        if (u == null || !encoder.matches(password, u.getPasswordHash())) {
            throw new ApiException(401, "用户名或密码错误");
        }
        if (!Boolean.TRUE.equals(u.getEnabled())) {
            throw new ApiException(403, "账号已被停用,请联系管理员");
        }
        return u;
    }

    @Transactional(readOnly = true)
    public AppUser getByUsername(String username) {
        return repo.findByUsername(username)
                .orElseThrow(() -> new ApiException(401, "账号不存在"));
    }

    // ---------- 管理端:老师账号管理 ----------

    @Transactional(readOnly = true)
    public PageResponse<AppUserResponse> listPaged(String keyword, int page, int size) {
        Specification<AppUser> spec = (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return cb.conjunction();
            String kw = "%" + keyword.trim() + "%";
            return cb.or(
                    cb.like(root.get("username"), kw),
                    cb.like(root.get("displayName"), kw),
                    cb.like(root.get("phone"), kw)
            );
        };
        int s = size <= 0 ? 20 : Math.min(size, 100);
        Page<AppUser> p = repo.findAll(spec,
                PageRequest.of(Math.max(page, 0), s, Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.of(p.map(u -> AppUserResponse.from(u, adminRepo.existsByUsername(u.getUsername()))));
    }

    @Transactional
    public void setEnabled(Long id, boolean enabled) {
        AppUser u = repo.findById(id).orElseThrow(() -> new ApiException(404, "账号不存在"));
        u.setEnabled(enabled);
        repo.save(u);
        if (!enabled) tokenStore.revokeUser(u.getUsername(), TokenStore.ROLE_USER);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new ApiException(400, "新密码至少 6 位");
        }
        AppUser u = repo.findById(id).orElseThrow(() -> new ApiException(404, "账号不存在"));
        u.setPasswordHash(encoder.encode(newPassword));
        repo.save(u);
        tokenStore.revokeUser(u.getUsername(), TokenStore.ROLE_USER); // 改密后需重新登录
    }

    @Transactional
    public void delete(Long id) {
        AppUser u = repo.findById(id).orElseThrow(() -> new ApiException(404, "账号不存在"));
        tokenStore.revokeUser(u.getUsername(), TokenStore.ROLE_USER);
        repo.delete(u);
    }

    /** 把老师提升为管理员:用其现有用户名与密码创建管理员账号,保留老师账号 */
    @Transactional
    public void promoteToAdmin(Long id) {
        AppUser u = repo.findById(id).orElseThrow(() -> new ApiException(404, "账号不存在"));
        if (adminRepo.existsByUsername(u.getUsername())) {
            throw new ApiException(400, "该账号已是管理员");
        }
        AdminUser a = new AdminUser();
        a.setUsername(u.getUsername());
        a.setPasswordHash(u.getPasswordHash()); // 沿用老师原密码
        a.setDisplayName(StringUtils.hasText(u.getDisplayName()) ? u.getDisplayName() : u.getUsername());
        a.setEnabled(true);
        adminRepo.save(a);
    }
}
