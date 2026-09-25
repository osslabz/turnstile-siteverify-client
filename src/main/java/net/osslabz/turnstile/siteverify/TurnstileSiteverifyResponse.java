package net.osslabz.turnstile.siteverify;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class TurnstileSiteverifyResponse {

    private String action;

    private boolean success;

    @JsonProperty("challenge_ts")
    private ZonedDateTime challengeTs;

    private String hostname;

    @JsonProperty("error-codes")
    private List<String> errorCodes = new ArrayList<>(3);
}
