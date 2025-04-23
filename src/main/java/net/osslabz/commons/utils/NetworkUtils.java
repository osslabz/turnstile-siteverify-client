package net.osslabz.commons.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NetworkUtils {

    private static final Logger log = LoggerFactory.getLogger(NetworkUtils.class);

    // Common headers that might contain the real IP address
    private static final List<String> IP_HEADERS = Arrays.asList(
        "CF-Connecting-IP",     // Cloudflare
        "X-Forwarded-For",      // Common proxy header
        "X-Real-IP",            // Nginx
        "True-Client-IP",       // Akamai and Cloudflare
        "X-Cluster-Client-IP",  // Rackspace, Riverbed
        "Fastly-Client-IP",     // Fastly
        "X-Forwarded",          // Generic forward
        "Forwarded-For",        // Generic forward
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
            if (ip != null && !ip.trim().isEmpty() && !ip.equalsIgnoreCase("unknown")) {
                if (header.equalsIgnoreCase("X-Forwarded-For") && ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }


    private static boolean isValidIpAddress(String ip) {

        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // Remove IPv6 zone index if present
        if (ip.contains("%")) {
            ip = ip.split("%")[0];
        }

        try {
            // Try to create an InetAddress - this validates both IPv4 and IPv6
            InetAddress addr = InetAddress.getByName(ip);

            // Additional checks for special/reserved addresses
            if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() ||
                addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
                log.warn("IP address {} is a special/reserved address", ip);
                return false;
            }

            return true;
        } catch (UnknownHostException e) {
            log.warn("Invalid IP address format: {}", ip);
            return false;
        }
    }
}