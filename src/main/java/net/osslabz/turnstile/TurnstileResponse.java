package net.osslabz.turnstile;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TurnstileResponse {

    private String action;

    private boolean success;

    @JsonProperty("challenge_ts")
    private ZonedDateTime challengeTs;

    private String hostname;

    private List<String> errorCodes = new ArrayList<>(3);
}
