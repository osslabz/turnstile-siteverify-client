package net.osslabz.turnstile;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TurnstileClient {
    private static final String SITEVERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER;
    private static final OkHttpClient DEFAULT_OKHTTP_CLIENT;
    public static final int DEFAULT_TIMEOUT = 30;

    static {
        DEFAULT_OBJECT_MAPPER = new ObjectMapper();
        DEFAULT_OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
                message -> LoggerFactory.getLogger(TurnstileClient.class).debug(message)
        );
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        DEFAULT_OKHTTP_CLIENT = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();
    }

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String secretKey;

    public TurnstileClient(String secretKey) {
        this(DEFAULT_OKHTTP_CLIENT, DEFAULT_OBJECT_MAPPER, secretKey);
    }

    public TurnstileClient(OkHttpClient httpClient, ObjectMapper objectMapper, String secretKey) {
        this.secretKey = secretKey;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public boolean isValid(String action, String token, String connectingIp) {
        TurnstileResponse turnstileResponse = this.verify(action, token, connectingIp);
        return turnstileResponse.isSuccess() && turnstileResponse.getErrorCodes().isEmpty() && Objects.equals(action,
                turnstileResponse.getAction());
    }

    public TurnstileResponse verify(String action, String token, String connectingIp) {
        RequestBody formBody = new FormBody.Builder()
                .add("secret", this.secretKey)
                .add("response", token)
                .add("remoteip", connectingIp)
                .build();

        Request request = new Request.Builder()
                .url(SITEVERIFY_URL)
                .post(formBody)
                .build();

        try (Response httpResponse = httpClient.newCall(request).execute()) {

            String responseBody = httpResponse.body() != null ? httpResponse.body().toString() : null;
            if (!httpResponse.isSuccessful() || responseBody == null) {
                throw new TurnstileException("Unexpected http response. code=%d, body='%s'".formatted(httpResponse.code(), responseBody));
            }

            return objectMapper.readValue(responseBody, TurnstileResponse.class);
        } catch (Exception e) {
            throw new TurnstileException(e);
        }
    }


}