package net.osslabz.turnstile.siteverify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import net.osslabz.testing.StubHttpServletRequest;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class TurnstileSiteverifyClientTest {

    private static final String SECRET = "test-secret";

    private static final String ACTION = "login";

    private static final String TOKEN = "token-123";

    private static final String CLIENT_IP = "203.0.113.7";

    private static final String SUCCESS_BODY = """
            {"success":true,"challenge_ts":"2026-09-24T10:15:30.123Z","hostname":"example.com",\
            "error-codes":[],"action":"login","cdata":"session-1"}""";

    private final List<HttpUrl> requestedUrls = new CopyOnWriteArrayList<>();

    private final Logger clientLogger = (Logger) LoggerFactory.getLogger(TurnstileSiteverifyClient.class);

    private final ListAppender<ILoggingEvent> clientLog = new ListAppender<>();

    private MockWebServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @BeforeEach
    void captureClientLog() {
        clientLog.start();
        clientLogger.addAppender(clientLog);
        clientLogger.setAdditive(false);
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @AfterEach
    void releaseClientLog() {
        clientLogger.setAdditive(true);
        clientLogger.detachAppender(clientLog);
    }

    /** Sends every call to the local server and remembers the URL the client asked for. */
    private OkHttpClient.Builder httpClientRoutedToServer() {
        return new OkHttpClient.Builder().addInterceptor(chain -> {
            HttpUrl requested = chain.request().url();
            requestedUrls.add(requested);
            HttpUrl local = server.url(requested.encodedPath());
            return chain.proceed(chain.request().newBuilder().url(local).build());
        });
    }

    private TurnstileSiteverifyClient client() {
        return new TurnstileSiteverifyClient(httpClientRoutedToServer().build(), SECRET);
    }

    private void respond(int code, String body) {
        server.enqueue(new MockResponse.Builder().code(code).body(body).build());
    }

    private void assertLoggedFailure(String expectedCause) {
        assertEquals(1, clientLog.list.size());
        String message = clientLog.list.get(0).getFormattedMessage();
        assertTrue(message.startsWith("call to siteverify failed: " + expectedCause), message);
    }

    private RecordedRequest takeRequest() throws InterruptedException {
        return server.takeRequest(5, TimeUnit.SECONDS);
    }

    @Test
    void acceptsTokenWhenSiteverifyReportsSuccessForTheExpectedAction() {
        respond(200, SUCCESS_BODY);

        assertTrue(client().isValid(ACTION, TOKEN, CLIENT_IP));
    }

    @Test
    void postsSecretTokenAndClientIpAsFormToCloudflareSiteverify() throws InterruptedException {
        respond(200, SUCCESS_BODY);

        client().isValid(ACTION, "a+b/c=d&e f", CLIENT_IP);

        RecordedRequest request = takeRequest();
        assertEquals(List.of(HttpUrl.get("https://challenges.cloudflare.com/turnstile/v0/siteverify")), requestedUrls);
        assertEquals("POST", request.getMethod());
        assertEquals("application/x-www-form-urlencoded", request.getHeaders().get("Content-Type"));
        assertEquals(
                "secret=test-secret&response=a%2Bb%2Fc%3Dd%26e+f&remoteip=203.0.113.7",
                request.getBody().string(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsTokenIssuedForAnotherAction() {
        respond(200, SUCCESS_BODY);

        assertFalse(client().isValid("signup", TOKEN, CLIENT_IP));
    }

    @Test
    void rejectsTokenWhenSiteverifyReportsFailure() {
        respond(200, """
                {"success":false,"error-codes":["invalid-input-response"]}""");

        assertFalse(client().isValid(ACTION, TOKEN, CLIENT_IP));
        assertTrue(clientLog.list.isEmpty());
    }

    @Test
    void rejectsTokenWhenSiteverifyReportsErrorCodesDespiteSuccess() {
        respond(200, """
                {"success":true,"hostname":"example.com","error-codes":["internal-error"],"action":"login"}""");

        assertFalse(client().isValid(ACTION, TOKEN, CLIENT_IP));
        assertTrue(clientLog.list.isEmpty());
    }

    @Test
    void rejectsTokenWhenSiteverifyAnswersWithHttpError() {
        respond(500, "internal error");

        assertFalse(client().isValid(ACTION, TOKEN, CLIENT_IP));
        assertLoggedFailure(TurnstileSiteverifyException.class.getName()
                + ": Unexpected http response. code=500, body='internal error'");
    }

    @Test
    void rejectsTokenWhenSiteverifyAnswersWithMalformedJson() {
        respond(200, "{\"success\":tru");

        assertFalse(client().isValid(ACTION, TOKEN, CLIENT_IP));
        assertLoggedFailure("com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'tru'");
    }

    @Test
    void rejectsTokenWhenSiteverifyDoesNotAnswerWithinTheReadTimeout() {
        server.enqueue(new MockResponse.Builder()
                .body(SUCCESS_BODY)
                .headersDelay(1, TimeUnit.SECONDS)
                .build());
        OkHttpClient impatientClient =
                httpClientRoutedToServer().readTimeout(Duration.ofMillis(200)).build();

        assertFalse(new TurnstileSiteverifyClient(impatientClient, SECRET).isValid(ACTION, TOKEN, CLIENT_IP));
        // The message depends on where the read blocks: "timeout" from okio, "Read timed out" from the socket.
        assertLoggedFailure("java.net.SocketTimeoutException");
    }

    @Test
    void parsesTheResponseWithTheGivenObjectMapper() {
        respond(200, SUCCESS_BODY);
        ObjectMapper strictMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        assertFalse(new TurnstileSiteverifyClient(httpClientRoutedToServer().build(), strictMapper, SECRET)
                .isValid(ACTION, TOKEN, CLIENT_IP));
        assertLoggedFailure("com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException");
    }

    @Test
    void verifiesTokenAndClientIpTakenFromTheServletRequest() throws InterruptedException {
        respond(200, SUCCESS_BODY);

        boolean valid = client().isValid(
                        ACTION,
                        StubHttpServletRequest.create(
                                Map.of("cf-turnstile-response", TOKEN),
                                Map.of("CF-Connecting-IP", CLIENT_IP),
                                "192.0.2.1"));

        assertTrue(valid);
        assertEquals(
                "secret=test-secret&response=token-123&remoteip=203.0.113.7",
                takeRequest().getBody().string(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsServletRequestWithoutTokenWithoutCallingSiteverify() {
        assertFalse(client().isValid(ACTION, StubHttpServletRequest.create(Map.of(), Map.of(), "192.0.2.1")));
        assertFalse(client().isValid(
                        ACTION,
                        StubHttpServletRequest.create(Map.of("cf-turnstile-response", ""), Map.of(), "192.0.2.1")));

        assertEquals(0, server.getRequestCount());
    }
}
