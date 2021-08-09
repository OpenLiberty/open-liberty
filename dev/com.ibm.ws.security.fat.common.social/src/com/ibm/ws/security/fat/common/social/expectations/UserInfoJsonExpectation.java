package com.ibm.ws.security.fat.common.social.expectations;

import java.io.StringReader;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue.ValueType;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants.CheckType;
import com.ibm.ws.security.fat.common.Constants.JsonCheckType;
import com.ibm.ws.security.fat.common.expectations.JsonObjectExpectation;
import com.ibm.ws.security.fat.common.social.apps.formlogin.BaseServlet;
import com.ibm.ws.security.fat.common.utils.FatStringUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

public class UserInfoJsonExpectation extends JsonObjectExpectation {

    public static final String SEARCH_LOCATION = "user-info-string";
    public static final String SERVLET_OUTPUT_PREFIX = BaseServlet.OUTPUT_PREFIX + "string: ";
    public static final String USER_INFO_SERVLET_OUTPUT_REGEX = Pattern.quote(SERVLET_OUTPUT_PREFIX) + "(\\{.*\\})";

    static final String DEFAULT_FAILURE_MSG = "An error occurred validating the UserInfo string.";

    public UserInfoJsonExpectation(String expectedKey) {
        this(expectedKey, JsonCheckType.KEY_EXISTS, null);
    }

    public UserInfoJsonExpectation(String expectedKey, CheckType checkType, Object expectedValue) {
        super(expectedKey, checkType, expectedValue);
        this.searchLocation = SEARCH_LOCATION;
        this.failureMsg = DEFAULT_FAILURE_MSG;
    }

    public UserInfoJsonExpectation(String expectedKey, ValueType expectedValueType) {
        super(expectedKey, expectedValueType);
        this.searchLocation = SEARCH_LOCATION;
        this.failureMsg = DEFAULT_FAILURE_MSG;
    }

    public UserInfoJsonExpectation(String expectedKey, ValueType expectedValueType, Object expectedValue) {
        super(expectedKey, expectedValueType, expectedValue, DEFAULT_FAILURE_MSG);
        this.searchLocation = SEARCH_LOCATION;
    }

    @Override
    protected JsonObject readJsonFromContent(Object contentToValidate) throws Exception {
        String method = "readJsonFromContent";
        String responseText = "FAILED TO READ RESPONSE TEXT";
        try {
            responseText = WebResponseUtils.getResponseText(contentToValidate);
            String userInfoJsonString = FatStringUtils.extractRegexGroup(responseText, getRegexForExtractingUserInfoString());
            Log.info(getClass(), method, "Extracted UserInfo string: [" + userInfoJsonString + "]");
            JsonObject userInfo = Json.createReader(new StringReader(userInfoJsonString)).readObject();
            Log.info(getClass(), method, "Resulting JSON object: [" + userInfo + "]");
            return userInfo;
        } catch (Exception e) {
            throw new Exception("Failed to read JSON data from the provided content. Error was: [" + e + "]. Content was: [" + responseText + "]");
        }
    }

    protected String getRegexForExtractingUserInfoString() {
        return USER_INFO_SERVLET_OUTPUT_REGEX;
    }

}
