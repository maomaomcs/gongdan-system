package com.school.ticket.service;

import com.school.ticket.config.AppProperties;
import com.school.ticket.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.school.ticket.entity.Ticket;
import com.school.ticket.entity.TicketLog;
import com.school.ticket.repository.TicketLogRepository;
import com.school.ticket.repository.TicketRepository;
import com.school.ticket.web.ApiException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepo;
    private final TicketLogRepository logRepo;
    private final AppProperties props;
    private final DingTalkNotifier dingTalkNotifier;
    private final FileStorageService fileStorage;
    private final SettingService settingService;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ---------- 创建 ----------
    @Transactional
    public TicketResponse create(CreateTicketRequest req, Long userId) {
        Ticket t = new Ticket();
        t.setUserId(userId);
        t.setReporter(req.reporter().trim());
        t.setContact(trimOrNull(req.contact()));
        t.setLocation(req.location().trim());
        t.setCategory(req.category().trim());
        t.setTitle(req.title().trim());
        t.setDescription(trimOrNull(req.description()));
        t.setUrgency("紧急".equals(req.urgency()) ? "紧急" : "普通");
        t.setStatus("待处理");
        t.setImages(sanitizeImages(req.images()));
        t.setCode(generateCode());
        ticketRepo.save(t);
        dingTalkNotifier.notifyNewTicketAsync(t); // 异步推送钉钉通知
        return TicketResponse.from(t, List.of());
    }

    private String generateCode() {
        for (int i = 0; i < 6; i++) {
            String code = "BX" + LocalDateTime.now().format(YMD) + "-"
                    + String.format("%04X", ThreadLocalRandom.current().nextInt(0x10000));
            if (ticketRepo.findByCode(code).isEmpty()) return code;
        }
        // 兜底:加时间戳后缀
        return "BX" + LocalDateTime.now().format(YMD) + "-" + System.currentTimeMillis();
    }

    // ---------- 我的报修(按用户,分页 + 筛选) ----------
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> listByUser(Long userId, String status, String keyword, int page, int size) {
        Specification<Ticket> spec = buildSpec(userId, status, null, null, null, keyword);
        Page<Ticket> p = ticketRepo.findAll(spec, PageRequest.of(Math.max(page, 0), clampSize(size)));
        return PageResponse.of(p.map(TicketResponse::from));
    }

    @Transactional(readOnly = true)
    public TicketResponse getByIdForUser(Long id, Long userId) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new ApiException(404, "工单不存在"));
        if (!userId.equals(t.getUserId())) {
            throw new ApiException(403, "无权查看该工单");
        }
        return withLogs(t);
    }

    /** 报修人催单:仅本人、仅未完成工单;两小时内只能催一次 */
    @Transactional
    public TicketResponse urge(Long id, Long userId) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new ApiException(404, "工单不存在"));
        if (!userId.equals(t.getUserId())) {
            throw new ApiException(403, "无权操作该工单");
        }
        if ("已解决".equals(t.getStatus()) || "已关闭".equals(t.getStatus())) {
            throw new ApiException(400, "工单已处理完成,无需催单");
        }
        LocalDateTime now = LocalDateTime.now();
        if (t.getLastUrgedAt() != null && t.getLastUrgedAt().isAfter(now.minusHours(2))) {
            throw new ApiException(400, "刚刚已经催过了,请稍后再催");
        }
        t.setUrgeCount((t.getUrgeCount() == null ? 0 : t.getUrgeCount()) + 1);
        t.setLastUrgedAt(now);
        ticketRepo.save(t);

        TicketLog entry = new TicketLog();
        entry.setTicketId(t.getId());
        entry.setContent("报修人催单(第 " + t.getUrgeCount() + " 次)");
        entry.setAuthor(t.getReporter());
        logRepo.save(entry);

        dingTalkNotifier.notifyUrgeAsync(t); // 催单也推钉钉

        return withLogs(t);
    }

    // ---------- 查询(公开,按工单号) ----------
    @Transactional(readOnly = true)
    public TicketResponse getByCode(String code) {
        Ticket t = ticketRepo.findByCode(code)
                .orElseThrow(() -> new ApiException(404, "未找到该工单号"));
        return withLogs(t);
    }

    // ---------- 详情(后台) ----------
    @Transactional(readOnly = true)
    public TicketResponse getById(Long id) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new ApiException(404, "工单不存在"));
        return withLogs(t);
    }

    private TicketResponse withLogs(Ticket t) {
        List<TicketLogResponse> logs = logRepo.findByTicketIdOrderByIdAsc(t.getId())
                .stream().map(TicketLogResponse::from).toList();
        return TicketResponse.from(t, logs);
    }

    /** 构造查询条件(带排序);userId 非空时只查该用户的工单 */
    private Specification<Ticket> buildSpec(Long userId, String status, String category,
                                            String urgency, String location, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (userId != null) ps.add(cb.equal(root.get("userId"), userId));
            if (StringUtils.hasText(status)) ps.add(cb.equal(root.get("status"), status));
            if (StringUtils.hasText(category)) ps.add(cb.equal(root.get("category"), category));
            if (StringUtils.hasText(urgency)) ps.add(cb.equal(root.get("urgency"), urgency));
            if (StringUtils.hasText(location)) ps.add(cb.like(root.get("location"), "%" + location + "%"));
            if (StringUtils.hasText(keyword)) {
                String kw = "%" + keyword + "%";
                ps.add(cb.or(
                        cb.like(root.get("title"), kw),
                        cb.like(root.get("description"), kw),
                        cb.like(root.get("reporter"), kw),
                        cb.like(root.get("location"), kw),
                        cb.like(root.get("code"), kw)
                ));
            }
            // 仅对数据查询设置排序(计数查询结果类型为 Long,跳过)
            // 最新提交的工单排在最上面(按 id 倒序,id 随创建时间递增)
            if (Ticket.class.equals(query.getResultType())) {
                query.orderBy(cb.desc(root.get("id")));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    // ---------- 列表(后台,分页 + 多条件筛选) ----------
    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> listPaged(String status, String category, String urgency,
                                                  String location, String keyword, int page, int size) {
        Specification<Ticket> spec = buildSpec(null, status, category, urgency, location, keyword);
        Page<Ticket> p = ticketRepo.findAll(spec, PageRequest.of(Math.max(page, 0), clampSize(size)));
        int oh = settingService.getOverdueHours();
        return PageResponse.of(p.map(t -> TicketResponse.from(t, oh)));
    }

    // ---------- 全部(导出用,不分页) ----------
    @Transactional(readOnly = true)
    public List<TicketResponse> listAll(String status, String category, String urgency, String location, String keyword) {
        Specification<Ticket> spec = buildSpec(null, status, category, urgency, location, keyword);
        return ticketRepo.findAll(spec).stream().map(TicketResponse::from).toList();
    }

    private int clampSize(int size) {
        if (size <= 0) return 20;
        return Math.min(size, 100);
    }

    // ---------- 更新(后台) ----------
    @Transactional
    public TicketResponse update(Long id, UpdateTicketRequest req) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new ApiException(404, "工单不存在"));
        if (req.status() != null) {
            if (!props.getStatuses().contains(req.status()))
                throw new ApiException(400, "非法状态");
            t.setStatus(req.status());
            if ("已解决".equals(req.status()) && t.getResolvedAt() == null) {
                t.setResolvedAt(LocalDateTime.now());
            }
        }
        if (req.handler() != null) t.setHandler(trimOrNull(req.handler()));
        if (req.resolution() != null) t.setResolution(trimOrNull(req.resolution()));
        ticketRepo.save(t);
        return withLogs(t);
    }

    // ---------- 添加跟进 ----------
    @Transactional
    public TicketResponse addLog(Long id, AddLogRequest req) {
        Ticket t = ticketRepo.findById(id)
                .orElseThrow(() -> new ApiException(404, "工单不存在"));
        TicketLog log = new TicketLog();
        log.setTicketId(t.getId());
        log.setContent(req.content().trim());
        log.setAuthor(trimOrNull(req.author()));
        logRepo.save(log);
        t.setUpdatedAt(LocalDateTime.now());
        ticketRepo.save(t);
        return withLogs(t);
    }

    // ---------- 统计 ----------
    @Transactional(readOnly = true)
    public StatsResponse stats() {
        long total = ticketRepo.count();
        long open = ticketRepo.countByStatusIn(List.of("待处理", "处理中"));
        Double avgSec = ticketRepo.avgResolveSeconds();
        Double avgHours = avgSec == null ? null : Math.round(avgSec / 3600.0 * 10) / 10.0;
        return new StatsResponse(
                total, open, avgHours,
                toBuckets(ticketRepo.countByStatus()),
                toBuckets(ticketRepo.countByCategory()),
                limit(toBuckets(ticketRepo.countByLocation()), 15),
                toBuckets(ticketRepo.countByMonth())
        );
    }

    private List<StatsResponse.Bucket> toBuckets(List<Object[]> rows) {
        List<StatsResponse.Bucket> out = new ArrayList<>();
        for (Object[] r : rows) {
            String name = r[0] == null ? "(未填)" : r[0].toString();
            long c = ((Number) r[1]).longValue();
            out.add(new StatsResponse.Bucket(name, c));
        }
        return out;
    }

    private List<StatsResponse.Bucket> limit(List<StatsResponse.Bucket> list, int n) {
        return list.size() > n ? list.subList(0, n) : list;
    }

    /** 只保留纯文件名(去掉任何路径),最多 6 张,防目录穿越 */
    private String sanitizeImages(java.util.List<String> images) {
        if (images == null || images.isEmpty()) return null;
        java.util.List<String> clean = new ArrayList<>();
        for (String s : images) {
            if (s == null) continue;
            String name = s.replace("\\", "/");
            name = name.substring(name.lastIndexOf('/') + 1).trim();
            // 必须是合法文件名且确实已上传到服务器,才接受
            if (fileStorage.exists(name)) clean.add(name);
            if (clean.size() >= 6) break;
        }
        return clean.isEmpty() ? null : String.join(",", clean);
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
