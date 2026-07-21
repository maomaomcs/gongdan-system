package com.school.ticket.service;

import com.school.ticket.entity.InviteCode;
import com.school.ticket.repository.InviteCodeRepository;
import com.school.ticket.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;

/** 注册邀请码(校验码)管理 */
@Service
@RequiredArgsConstructor
public class InviteCodeService {

    private final InviteCodeRepository repo;
    private final SecureRandom random = new SecureRandom();
    // 去掉易混淆字符(0/O、1/I/L)
    private static final char[] ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();

    @Transactional(readOnly = true)
    public List<InviteCode> list() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(InviteCode::getId).reversed())
                .toList();
    }

    /** 生成一个新的邀请码 */
    @Transactional
    public InviteCode generate(String note, Integer maxUses) {
        InviteCode c = new InviteCode();
        c.setCode(randomCode());
        c.setNote(StringUtils.hasText(note) ? note.trim() : null);
        c.setMaxUses(maxUses == null || maxUses < 0 ? 0 : maxUses);
        c.setUsedCount(0);
        c.setEnabled(true);
        return repo.save(c);
    }

    @Transactional
    public void setEnabled(Long id, boolean enabled) {
        InviteCode c = repo.findById(id).orElseThrow(() -> new ApiException(404, "邀请码不存在"));
        c.setEnabled(enabled);
        repo.save(c);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ApiException(404, "邀请码不存在");
        repo.deleteById(id);
    }

    /**
     * 校验并消耗一个邀请码(注册时调用,须在事务内)。
     * 校验失败抛出 ApiException。
     */
    @Transactional
    public void validateAndConsume(String code) {
        if (!StringUtils.hasText(code)) {
            throw new ApiException(400, "请填写邀请码");
        }
        InviteCode c = repo.findByCode(code.trim()).orElseThrow(() -> new ApiException(400, "邀请码无效"));
        if (!Boolean.TRUE.equals(c.getEnabled())) {
            throw new ApiException(400, "该邀请码已停用");
        }
        if (c.getMaxUses() != null && c.getMaxUses() > 0 && c.getUsedCount() >= c.getMaxUses()) {
            throw new ApiException(400, "该邀请码使用次数已用尽");
        }
        c.setUsedCount(c.getUsedCount() + 1);
        repo.save(c);
    }

    private String randomCode() {
        String code;
        int guard = 0;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            code = sb.toString();
        } while (repo.existsByCode(code) && ++guard < 20);
        return code;
    }
}
