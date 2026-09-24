package net.osslabz.testing;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.TreeMap;

/** A servlet request that answers only the parameter, header and remote address lookups. */
public final class StubHttpServletRequest {

    private StubHttpServletRequest() {}

    public static HttpServletRequest withHeaders(Map<String, String> headers, String remoteAddr) {
        return create(Map.of(), headers, remoteAddr);
    }

    public static HttpServletRequest create(
            Map<String, String> parameters, Map<String, String> headers, String remoteAddr) {
        // Servlet containers match header names case-insensitively.
        Map<String, String> caseInsensitiveHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveHeaders.putAll(headers);
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getParameter" -> parameters.get((String) args[0]);
                    case "getHeader" -> caseInsensitiveHeaders.get((String) args[0]);
                    case "getRemoteAddr" -> remoteAddr;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
