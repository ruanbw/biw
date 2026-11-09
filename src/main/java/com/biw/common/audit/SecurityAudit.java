package com.biw.common.audit;

import cn.dev33.satoken.stp.StpUtil;
import com.biw.common.web.ClientAddress;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication and authorization events: failed logins, lockouts, refused requests.
 * <p>
 * Written to its own logger, {@code com.biw.security.audit}, so a deployment can route it to a
 * separate appender and alert on it. The rest of the application log is not a place to look for
 * these: business errors are logged at debug and are absent from a production log.
 * <p>
 * Never pass a password or a session token into {@code detail}: this log is the one that is kept
 * and shipped.
 */
public final class SecurityAudit {

    private static final Logger log = LoggerFactory.getLogger("com.biw.security.audit");

    private SecurityAudit() {
    }

    /**
     * @param event  stable machine-readable name, e.g. {@code auth.login.failed}
     * @param detail fields specific to the event; must not contain credentials
     */
    public static void failure(String event, String detail) {
        HttpServletRequest request = ClientAddress.currentRequest();
        log.warn("event={} method={} path={} ip={} user={} detail={}",
                event,
                request == null ? ClientAddress.UNKNOWN : request.getMethod(),
                request == null ? ClientAddress.UNKNOWN : request.getRequestURI(),
                ClientAddress.current(),
                currentUserId(),
                oneLine(detail));
    }

    /**
     * Keeps the detail on a single short line. The login path passes a username that the request
     * only bounds by length, so it may carry newlines and would otherwise forge log records.
     */
    private static String oneLine(String detail) {
        if (detail == null) {
            return ClientAddress.UNKNOWN;
        }
        String single = detail.replace('\r', '_').replace('\n', '_');
        return single.length() <= 200 ? single : single.substring(0, 200) + "...";
    }

    /**
     * The session id of the caller, or {@code -} when the call is unauthenticated or outside a
     * request. Sa-Token throws when there is no request context, hence the guard.
     */
    private static String currentUserId() {
        try {
            return StpUtil.isLogin() ? String.valueOf(StpUtil.getLoginId()) : ClientAddress.UNKNOWN;
        } catch (RuntimeException e) {
            return ClientAddress.UNKNOWN;
        }
    }
}
