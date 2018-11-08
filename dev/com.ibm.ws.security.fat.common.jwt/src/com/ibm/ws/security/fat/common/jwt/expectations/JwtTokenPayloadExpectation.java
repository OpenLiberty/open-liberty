package com.ibm.ws.security.fat.common.jwt.expectations;

import javax.json.JsonObject;
import javax.json.JsonValue.ValueType;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants.CheckType;
import com.ibm.ws.security.fat.common.Constants.JsonCheckType;
import com.ibm.ws.security.fat.common.expectations.JsonObjectExpectation;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;

public class JwtTokenPayloadExpectation extends JsonObjectExpectation {

    public static final String SEARCH_LOCATION = "jwt-token-payload";

    static final String DEFAULT_FAILURE_MSG = "An error occurred validating the JWT Payload.";

    public JwtTokenPayloadExpectation(String expectedKey) {
        this(expectedKey, JsonCheckType.KEY_EXISTS, null);
    }

    public JwtTokenPayloadExpectation(String expectedKey, CheckType checkType, Object expectedValue) {
        super(expectedKey, checkType, expectedValue);
        this.searchLocation = SEARCH_LOCATION;
        this.failureMsg = DEFAULT_FAILURE_MSG;
    }

    public JwtTokenPayloadExpectation(String expectedKey, ValueType expectedValueType) {
        super(expectedKey, expectedValueType);
        this.searchLocation = SEARCH_LOCATION;
        this.failureMsg = DEFAULT_FAILURE_MSG;
    }

    public JwtTokenPayloadExpectation(String expectedKey, ValueType expectedValueType, Object expectedValue) {
        super(expectedKey, expectedValueType, expectedValue, DEFAULT_FAILURE_MSG);
        this.searchLocation = SEARCH_LOCATION;
    }

    @Override
    protected JsonObject readJsonFromContent(Object contentToValidate) throws Exception {
        String method = "readJsonFromContent (JwtTokenPayloadExpectation)";
        JsonObject payload = null;
        try {
            if (contentToValidate != null && (contentToValidate instanceof String)) {
                payload = (new JwtTokenForTest((String) contentToValidate)).getJsonPayload();
            }
            Log.info(thisClass, method, "Payload: " + payload);
            return payload;
        } catch (Exception e) {
            throw new Exception("Failed to read JSON data from the provided content. Error was: [" + e + "]. Content was: [" + contentToValidate + "]");
        }
    }

}
