package net.osslabz.commons.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

public class NetworkUtils {

    // Common headers that might contain the real IP address
    private static final List<String> IP_HEADERS = Arrays.asList(
            "CF-Connecting-IP", // Cloudflare
            "X-Forwarded-For", // Common proxy header
            "X-Real-IP", // Nginx
            "True-Client-IP", // Akamai and Cloudflare
            "X-Cluster-Client-IP", // Rackspace, Riverbed
            "Fastly-Client-IP", // Fastly
            "X-Forwarded", // Generic forward
            "Forwarded-For", // Generic forward
            "X-Original-Forwarded-For" // Original forwarded
            );

    /**
     * Retrieves the client's IP address from the request, taking into account various proxy headers
     *
     * @param request The HttpServletRequest
     * @return The client's IP address as a String, or null if no valid IP could be found
     */
    public static String getClientIpAddress(HttpServletRequest request) {

        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.trim().isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if ("X-Forwarded-For".equalsIgnoreCase(header) && ip.contains(",")) {
                    ip = ip.substring(0, ip.indexOf(',')).trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}
