package net.osslabz.commons.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import net.osslabz.testing.StubHttpServletRequest;
import org.junit.jupiter.api.Test;

class NetworkUtilsTest {

    private static final String REMOTE_ADDR = "192.0.2.1";

    private static String clientIp(Map<String, String> headers) {
        return NetworkUtils.getClientIpAddress(StubHttpServletRequest.withHeaders(headers, REMOTE_ADDR));
    }

    @Test
    void returnsRemoteAddressWhenNoProxyHeaderIsSet() {
        assertEquals(REMOTE_ADDR, clientIp(Map.of()));
    }

    @Test
    void prefersCloudflareConnectingIpOverOtherProxyHeaders() {
        assertEquals(
                "203.0.113.7", clientIp(Map.of("X-Forwarded-For", "198.51.100.2", "CF-Connecting-IP", "203.0.113.7")));
    }

    @Test
    void returnsFirstEntryOfForwardedForList() {
        assertEquals("203.0.113.7", clientIp(Map.of("X-Forwarded-For", "203.0.113.7 , 10.0.0.1, 10.0.0.2")));
    }

    @Test
    void keepsCommaListInOtherHeadersAsItIs() {
        assertEquals("203.0.113.7, 10.0.0.1", clientIp(Map.of("X-Real-IP", "203.0.113.7, 10.0.0.1")));
    }

    @Test
    void skipsBlankAndUnknownHeaders() {
        assertEquals(
                "203.0.113.7",
                clientIp(Map.of(
                        "CF-Connecting-IP", "  ",
                        "X-Forwarded-For", "Unknown",
                        "X-Real-IP", "203.0.113.7")));
    }

    @Test
    void fallsBackToLowestPriorityHeader() {
        assertEquals("203.0.113.7", clientIp(Map.of("X-Original-Forwarded-For", "203.0.113.7")));
    }
}
