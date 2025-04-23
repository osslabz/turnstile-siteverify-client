package net.osslabz.turnstile.siteverify;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import net.osslabz.commons.utils.NetworkUtils;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TurnstileSiteverifyClient {

    private static final Logger log = LoggerFactory.getLogger(TurnstileSiteverifyClient.class);

    private static final String SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER;

    private static final OkHttpClient DEFAULT_OKHTTP_CLIENT;

    public static final int DEFAULT_TIMEOUT = 30;

    static {
        DEFAULT_OBJECT_MAPPER = new ObjectMapper();
        DEFAULT_OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        DEFAULT_OBJECT_MAPPER.registerModule(new JavaTimeModule());

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
            message -> LoggerFactory.getLogger(TurnstileSiteverifyClient.class).trace(message)
        );
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        DEFAULT_OKHTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build();
    }

    private final OkHttpClient httpClient;

    private final ObjectMapper objectMapper;

    private final String secretKey;


    public TurnstileSiteverifyClient(String secretKey) {

        this(DEFAULT_OKHTTP_CLIENT, DEFAULT_OBJECT_MAPPER, secretKey);
    }


    public TurnstileSiteverifyClient(OkHttpClient httpClient, String secretKey) {

        this(httpClient, DEFAULT_OBJECT_MAPPER, secretKey);
    }


    public TurnstileSiteverifyClient(OkHttpClient httpClient, ObjectMapper objectMapper, String secretKey) {

        this.secretKey = secretKey;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }


    public boolean isValid(String action, HttpServletRequest httpServletRequest) {

        String challengeResponseToken = httpServletRequest.getParameter("cf-turnstile-response");
        if (challengeResponseToken == null || challengeResponseToken.isEmpty()) {
            return false;
        }
        return this.isValid(action, challengeResponseToken, NetworkUtils.getClientIpAddress(httpServletRequest));
    }


    public boolean isValid(String action, String challengeResponseToken, String connectingIp) {

        try {
            TurnstileSiteverifyResponse turnstileResponse = this.verify(challengeResponseToken, connectingIp);
            return turnstileResponse.isSuccess() && turnstileResponse.getErrorCodes().isEmpty() && Objects.equals(action,
                turnstileResponse.getAction());
        } catch (Exception e) {
            log.debug("call to siteverify failed: {}", e.getMessage());
            return false;
        }
    }


    private TurnstileSiteverifyResponse verify(String challengeResponseToken, String connectingIp) {

        RequestBody formBody = new FormBody.Builder()
            .add("secret", this.secretKey)
            .add("response", challengeResponseToken)
            .add("remoteip", connectingIp)
            .build();

        Request request = new Request.Builder()
            .url(SITEVERIFY_URL)
            .post(formBody)
            .build();

        try (Response httpResponse = httpClient.newCall(request).execute()) {

            String responseBody = httpResponse.body() != null ? httpResponse.body().string() : null;
            if (!httpResponse.isSuccessful() || responseBody == null) {
                throw new TurnstileSiteverifyException("Unexpected http response. code=%d, body='%s'".formatted(httpResponse.code(), responseBody));
            }

            return objectMapper.readValue(responseBody, TurnstileSiteverifyResponse.class);
        } catch (Exception e) {
            throw new TurnstileSiteverifyException(e);
        }
    }
}