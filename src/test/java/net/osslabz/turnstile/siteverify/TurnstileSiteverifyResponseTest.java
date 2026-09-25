package net.osslabz.turnstile.siteverify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import org.junit.jupiter.api.Test;

class TurnstileSiteverifyResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void readsErrorCodesOfAFailedValidation() throws JsonProcessingException {
        String failureBody = """
                {"success":false,"error-codes":["invalid-input-response","timeout-or-duplicate"]}""";

        TurnstileSiteverifyResponse response = objectMapper.readValue(failureBody, TurnstileSiteverifyResponse.class);

        assertFalse(response.isSuccess());
        assertEquals(List.of("invalid-input-response", "timeout-or-duplicate"), response.getErrorCodes());
    }
}
