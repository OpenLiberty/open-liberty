package io.openliberty.security.jakartasec.fat.utils;

import com.ibm.websphere.simplicity.log.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class Utils {

    private static final Class<?> c = Utils.class;

    /**
     * Helper method to execute GET request with Basic Authentication.
     */
    public static String executeGetRequestBasicAuth(String url, String username, String password, int expectedStatusCode) throws Exception {
        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

        // Set Basic Authentication header
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

        conn.setRequestMethod("GET");
        conn.setDoInput(true);

        int responseCode = conn.getResponseCode();
        assertEquals("Expected status code " + expectedStatusCode + " but got " + responseCode,
                expectedStatusCode, responseCode);

        // Read response
        StringBuilder response = new StringBuilder();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        }

        conn.disconnect();

        Log.info(c, "executeGetRequestBasicAuth",
                "Request to " + url + " with user " + username + " returned status " + responseCode);

        return response.toString();
    }

    /**
     * Helper method for making requests without providing any authorizaiton - useful for RunAs resources
     * @param url
     * @param expectedStatusCode
     * @return
     * @throws Exception
     */
    public static String executeGetRequestNoAuth(String url, int expectedStatusCode) throws Exception {
        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

        conn.setRequestMethod("GET");
        conn.setDoInput(true);

        int responseCode = conn.getResponseCode();
        assertEquals("Expected status code " + expectedStatusCode + " but got " + responseCode,
                expectedStatusCode, responseCode);

        // Read response
        StringBuilder response = new StringBuilder();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        }

        conn.disconnect();

        Log.info(c, "executeGetRequestBasicAuth",
                "Request to " + url + " returned status " + responseCode);

        return response.toString();
    }

    public static Set<String> convertStringToSet(String input){
        input = input.replaceAll("^\\[","").replaceAll("]$","").trim();
        if (input.isBlank()){
            return Collections.EMPTY_SET;
        }
        return Arrays.stream(input.split(",")).map(String::trim).collect(Collectors.toSet());

    }
}
