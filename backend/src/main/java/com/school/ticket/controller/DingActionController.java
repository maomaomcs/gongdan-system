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
                      @RequestParam("s") String s,
                      @RequestParam(value = "c", required = false) String c) {
        if (!signer.verify(t, a, s)) {
            return failPage("该操作链接已失效或被篡改,请勿手动改动链接。");
        }
        String status, label;
        switch (a) {
            case "claim":   status = "处理中"; label = "认领(转处理中)"; break;
            case "resolve": status = "已解决"; label = "标记已解决"; break;
            case "cancel":  status = "已取消"; label = "取消工单"; break;
            default: return failPage("不支持的操作类型。");
        }
        // 第一步:未带确认标记 c=1 → 先展示确认框,避免误触
        if (!"1".equals(c)) {
            Ticket cur = ticketService.peek(t);
            if (cur == null) return failPage("工单不存在或已被删除。");
            return confirmPage(t, a, s, status, label, esc(cur.getCode()), esc(cur.getStatus()));
        }
        // 第二步:确认后执行
        try {
            Ticket tk = ticketService.updateStatusFromDing(t, status, label);
            return successPage(status, esc(tk.getCode()));
        } catch (ApiException e) {
            return failPage(esc(e.getMessage()));
        } catch (Exception e) {
            return failPage("系统异常,请稍后重试。");
        }
    }

    /** 确认框:显示工单号+当前状态+目标动作,点「确认」才真正执行 */
    private String confirmPage(Long t, String a, String s, String status, String label,
                               String code, String curStatus) {
        String main, light, icon;
        switch (status) {
            case "处理中": main = "#1d6fb8"; light = "#eaf3fb"; icon = "🙋"; break;
            case "已解决": main = "#2f9e44"; light = "#e9f7ee"; icon = "✅"; break;
            case "已取消": main = "#c07a12"; light = "#fbf1e0"; icon = "🚫"; break;
            default:      main = "#a4232a"; light = "#f7eee2"; icon = "❓"; break;
        }
        String confirmUrl = "/api/ding/act?t=" + t + "&a=" + a + "&s=" + s + "&c=1";
        String body =
                "<div style=\"font-size:14px;color:#6b6156;line-height:1.9\">工单 <b style=\"color:#3a332c\">" + code + "</b>"
                + "<br/>当前状态:<span style=\"color:#8a7f72\">" + curStatus + "</span></div>"
                + "<div style=\"margin:14px 0 4px;font-size:15px;color:#3a332c\">确认要将其<b style=\"color:" + main + "\"> " + esc(label) + " </b>吗?</div>"
                + "<div style=\"display:inline-block;margin-top:6px;padding:6px 18px;border-radius:999px;background:" + main
                + ";color:#fff;font-size:15px;font-weight:600\">→ " + status + "</div>"
                + "<div style=\"margin-top:26px;display:flex;gap:12px;justify-content:center\">"
                + "<a href=\"" + confirmUrl + "\" style=\"flex:1;max-width:150px;text-decoration:none;background:" + main
                + ";color:#fff;padding:12px 0;border-radius:10px;font-size:16px;font-weight:600;"
                + "box-shadow:0 4px 14px " + main + "55\">确 认</a>"
                + "<a href=\"javascript:history.back()\" onclick=\"try{window.close()}catch(e){}\" "
                + "style=\"flex:1;max-width:150px;text-decoration:none;background:#f0eadd;color:#6b6156;"
                + "padding:12px 0;border-radius:10px;font-size:16px\">再想想</a>"
                + "</div>";
        return render("操作确认", body, main, light, icon, true);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 成功页:按目标状态用不同配色/图标 */
    private String successPage(String status, String code) {
        String main, light, icon, verb;
        switch (status) {
            case "处理中":
                main = "#1d6fb8"; light = "#eaf3fb"; icon = "🙋"; verb = "已认领,开始处理"; break;
            case "已解决":
                main = "#2f9e44"; light = "#e9f7ee"; icon = "✅"; verb = "已标记解决"; break;
            case "已取消":
                main = "#c07a12"; light = "#fbf1e0"; icon = "🚫"; verb = "已取消"; break;
            default:
                main = "#a4232a"; light = "#f7eee2"; icon = "✅"; verb = "已更新"; break;
        }
        String body = "<div style=\"font-size:14px;color:#6b6156;line-height:1.8\">工单 <b style=\"color:#3a332c\">" + code
                + "</b><br/>当前状态</div>"
                + "<div style=\"display:inline-block;margin-top:12px;padding:7px 20px;border-radius:999px;"
                + "background:" + main + ";color:#fff;font-size:17px;font-weight:600;letter-spacing:1px;"
                + "box-shadow:0 4px 14px " + main + "55\">" + status + "</div>";
        return render(verb, body, main, light, icon, true);
    }

    /** 失败页:统一琥珀警示色 */
    private String failPage(String msg) {
        String body = "<p style=\"color:#6b6156;font-size:15px;line-height:1.7;margin:0\">" + msg + "</p>";
        return render("操作未生效", body, "#c0392b", "#fbebe8", "⚠️", false);
    }

    /**
     * 统一的石室风确认页骨架:顶部彩色状态圆环 + 标题 + 内容 + 页脚。
     * main=主题色, light=浅底色, icon=大图标。
     */
    private String render(String title, String bodyHtml, String main, String light, String icon, boolean ok) {
        return "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + esc(title) + "</title></head>"
                + "<body style=\"margin:0;font-family:-apple-system,'PingFang SC','Microsoft YaHei',sans-serif;"
                + "background:linear-gradient(160deg," + light + " 0%,#f6f1e7 60%);"
                + "display:flex;align-items:center;justify-content:center;min-height:100vh;padding:20px\">"
                + "<div style=\"background:#fffdf8;border-radius:18px;border-top:5px solid " + main + ";"
                + "box-shadow:0 10px 34px rgba(74,56,30,.14);padding:34px 30px 26px;max-width:340px;width:100%;text-align:center\">"
                // 彩色圆环 + 图标
                + "<div style=\"width:82px;height:82px;margin:0 auto 14px;border-radius:50%;"
                + "background:" + light + ";border:3px solid " + main + ";display:flex;align-items:center;justify-content:center;"
                + "font-size:40px;line-height:1\">" + icon + "</div>"
                + "<h2 style=\"font-family:'Noto Serif SC','STSong',serif;color:" + main + ";margin:6px 0 14px;font-size:22px\">"
                + esc(title) + "</h2>"
                + bodyHtml
                + "<div style=\"margin-top:24px;padding-top:14px;border-top:1px dashed #e3d8c3;"
                + "font-size:12px;color:#b7ab97\">石室联中132 · 后勤报修</div>"
                + "</div></body></html>";
    }
}
