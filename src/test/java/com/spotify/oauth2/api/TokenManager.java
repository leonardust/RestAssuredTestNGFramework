package com.spotify.oauth2.api;

import com.spotify.oauth2.utils.ConfigLoader;
import io.restassured.response.Response;
import lombok.extern.java.Log;

import java.time.Instant;
import java.util.HashMap;

@Log
public class TokenManager {

    private static String access_token;
    private static Instant expiry_time;

    public synchronized static String getToken() {
        try {
            if(access_token == null || Instant.now().isAfter(expiry_time)) {
                log.info("Renewing token ...");
                Response response = renewToken();
                access_token = response.path("access_token");
                int expiryTimeInSeconds = response.path("expires_in");
                expiry_time = Instant.now().plusSeconds(expiryTimeInSeconds - 300);
            } else {
                log.info("Token is valid");
            }

        } catch(Exception e) {
            throw new RuntimeException("ABORTED!!! Renew Token failed");
        }
        return access_token;
    }

    private static Response renewToken() {
        HashMap<String, String> formParams = new HashMap<>();
        formParams.put("client_id", ConfigLoader.getInstance().getClientId());
        formParams.put("client_secret", ConfigLoader.getInstance().getClientSecret());
        formParams.put("grant_type", ConfigLoader.getInstance().getGrantType());
        formParams.put("refresh_token", ConfigLoader.getInstance().getRefreshToken());
        Response response = RestResource.postAccount(formParams);

        if(response.statusCode() != 200) {
            throw new RuntimeException("ABORTED!!! Renew Token failed");
        }
        return response;
    }
}
