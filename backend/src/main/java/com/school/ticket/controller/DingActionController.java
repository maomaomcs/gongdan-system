package com.school.ticket.controller;

import com.school.ticket.entity.Ticket;
import com.school.ticket.service.DingActionSigner;
import com.school.ticket.service.TicketService;
import com.school.ticket.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 钉钉群按钮回调(公开,靠签名校验,不走登录拦截器)。
 * 群里点【认领/已解决/取消】→ 打开本页 → 同步平台状态 → 显示确认页。
 */
@RestController
@RequestMapping("/api/ding")
@RequiredArgsConstructor
public class DingActionController {

    private final DingActionSigner signer;
    private final TicketService ticketService;

    @GetMapping(value = "/act", produces = "text/html;charset=UTF-8")
    public String act(@RequestParam("t") Long t,
                      @RequestParam("a") String a,
                      @RequestParam("s") String s) {
        if (!signer.verify(t, a, s)) {
            return page("链接无效", "该操作链接已失效或被篡改,请勿手动改动链接。", false);
        }
        String status, label;
        switch (a) {
            case "claim":   status = "处理中"; label = "认领(转处理中)"; break;
            case "resolve": status = "已解决"; label = "标记已解决"; break;
            case "cancel":  status = "已取消"; label = "取消工单"; break;
            default: return page("未知操作", "不支持的操作类型。", false);
        }
        try {
            Ticket tk = ticketService.updateStatusFromDing(t, status, label);
            return page("操作成功", "工单 " + esc(tk.getCode()) + " 已更新为 【" + status + "】", true);
        } catch (ApiException e) {
            return page("操作失败", esc(e.getMessage()), false);
        } catch (Exception e) {
            return page("操作失败", "系统异常,请稍后重试。", false);
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 一个简单的石室风确认页 */
    private String page(String title, String msg, boolean ok) {
        String color = ok ? "#a4232a" : "#8c8c8c";
        String icon = ok ? "✅" : "⚠️";
        return "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + esc(title) + "</title></head>"
                + "<body style=\"margin:0;font-family:-apple-system,'PingFang SC','Microsoft YaHei',sans-serif;"
                + "background:#f6f1e7;display:flex;align-items:center;justify-content:center;min-height:100vh\">"
                + "<div style=\"background:#fffdf8;border:1px solid #e3d8c3;border-radius:16px;"
                + "box-shadow:0 8px 28px rgba(74,56,30,.1);padding:36px 30px;max-width:340px;text-align:center\">"
                + "<div style=\"font-size:52px;line-height:1\">" + icon + "</div>"
                + "<h2 style=\"font-family:'Noto Serif SC','STSong',serif;color:" + color + ";margin:16px 0 8px\">" + esc(title) + "</h2>"
                + "<p style=\"color:#6b6156;font-size:15px;line-height:1.7;margin:0\">" + msg + "</p>"
                + "<div style=\"margin-top:22px;font-size:12px;color:#b7ab97\">石室联中 · 后勤报修</div>"
                + "</div></body></html>";
    }
}
