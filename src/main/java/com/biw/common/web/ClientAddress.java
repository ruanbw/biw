package com.biw.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * The address the request came from, for rate limiting and audit records.
 * <p>
 * {@code server.forward-headers-strategy: native} in the prod profile makes the container rewrite
 * {@code getRemoteAddr()} from {@code X-Forwarded-For}, so behind a proxy this is the client and
 * not the proxy. Outside a request (a scheduled job, a unit test) there is no address.
 */
public final class ClientAddress {

    /** Shown instead of an address when the call is not on a request thread. */
    public static final String UNKNOWN = "-";

    private ClientAddress() {
    }

    public static String current() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return UNKNOWN;
        }
        String address = request.getRemoteAddr();
        return address == null || address.isBlank() ? UNKNOWN : address;
    }

    public static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }
}
