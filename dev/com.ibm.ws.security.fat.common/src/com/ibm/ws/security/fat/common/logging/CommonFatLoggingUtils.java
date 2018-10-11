package com.ibm.ws.security.fat.common.logging;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.apps.testmarker.TestMarker;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

import componenttest.topology.impl.LibertyServer;

public class CommonFatLoggingUtils {

    private final Class<?> thisClass = CommonFatLoggingUtils.class;

    public static final String PRINT_DELIMITER_CLASS_NAME = "#";
    public static final String PRINT_DELIMITER_METHOD_NAME = "*";
    public static final String PRINT_DELIMITER_REQUEST_PARTS = "@";

    private boolean clearBlankLinesFromResponseFull = true;

    public void setClearBlankLinesFromResponseFull(boolean clearLines) {
        clearBlankLinesFromResponseFull = clearLines;
    }

    public void logTestCaseInServerLog(LibertyServer server, String testName, String actionToLog) {
        try {
            if (server != null) {
                String parameters = TestMarker.PARAM_TEST_NAME + "=" + testName + "&" + TestMarker.PARAM_ACTION + "=" + actionToLog;
                HttpURLConnection connection = SecurityFatHttpUtils.getHttpConnectionWithAnyResponseCode(server, "/testmarker/testMarker?" + parameters);
                Log.info(thisClass, "logTestCaseInServerLog", connection.toString());
                SecurityFatHttpUtils.getResponseBody(connection);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printClassName(String className) {
        String delimiter = StringUtils.repeat(PRINT_DELIMITER_CLASS_NAME, 40);
        String msg = wrapInDelimiter("Starting test class: " + className, delimiter);
        printToLogAndSystemOut("printClassName", msg);
    }

    public void printMethodName(String methodName) {
        Log.info(thisClass, methodName, StringUtils.repeat(PRINT_DELIMITER_METHOD_NAME, 30) + " " + methodName);
    }

    public void printMethodName(String methodName, String task) {
        String delimiter = StringUtils.repeat(PRINT_DELIMITER_METHOD_NAME, 30);
        Log.info(thisClass, methodName, wrapInDelimiter(task + " " + methodName, delimiter));
    }

    public void printRequestParts(WebRequest request, String testName) {
        printRequestParts(null, request, testName);
    }

    public void printRequestParts(WebClient webClient, WebRequest request, String testName) {
        Log.info(thisClass, testName, StringUtils.center(" Start Request Parts ", 150, PRINT_DELIMITER_REQUEST_PARTS));
        if (request == null) {
            Log.info(thisClass, testName, "The request is null - nothing to print");
            return;
        }
        if (webClient != null) {
            printAllCookies(webClient);
        }
        printRequestInfo(request, testName);
        Log.info(thisClass, testName, StringUtils.center(" End Request Parts ", 150, PRINT_DELIMITER_REQUEST_PARTS));
    }

    public void printAllCookies(WebClient webClient) {
        printMethodName("printAllCookies");
        if (webClient == null) {
            return;
        }
        CookieManager cookieManager = webClient.getCookieManager();
        Set<Cookie> cookies = cookieManager.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                Log.info(thisClass, "printAllCookies", "Cookie: " + cookie.getName() + " Value: " + cookie.getValue());
            }
        }
    }

    void printRequestInfo(WebRequest request, String testName) {
        Log.info(thisClass, testName, "Request URL: " + request.getUrl());
        printRequestHeaders(request, testName);
        printRequestParameters(request, testName);
        printRequestBody(request, testName);
    }

    void printRequestHeaders(WebRequest request, String testName) {
        Map<String, String> requestHeaders = request.getAdditionalHeaders();
        if (requestHeaders != null) {
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                Log.info(thisClass, testName, "Request header: " + entry.getKey() + ", set to: " + entry.getValue());
            }
        }
    }

    void printRequestParameters(WebRequest request, String testName) {
        List<NameValuePair> requestParms = request.getRequestParameters();
        if (requestParms != null) {
            for (NameValuePair req : requestParms) {
                Log.info(thisClass, testName, "Request parameter: " + req.getName() + ", set to: " + req.getValue());
            }
        }
    }

    void printRequestBody(WebRequest request, String testName) {
        Log.info(thisClass, testName, "Request body: " + request.getRequestBody());
    }

    public void printResponseParts(Object response, String testName) throws Exception {
        printResponseParts(response, testName, null);
    }

    public void printResponseParts(Object response, String testName, String additionalMessage) throws Exception {
        Log.info(thisClass, testName, StringUtils.center(" Start Response Content ", 150, PRINT_DELIMITER_REQUEST_PARTS));
        if (response == null) {
            Log.info(thisClass, testName, "The response is null - nothing to print");
            return;
        }
        printResponseClass(response, testName);
        try {
            if (additionalMessage != null) {
                Log.info(thisClass, testName, additionalMessage);
            }
            printResponseStatusCode(response, testName);
            printResponseTitle(response, testName);
            printResponseUrl(response, testName);
            printResponseHeaders(response, testName);
            printResponseMessage(response, testName);
            printResponseText(response, testName);
        } catch (Exception e) {
            e.printStackTrace();
            Log.error(thisClass, testName, e, "Error printing response (log error and go on)");
        }
        Log.info(thisClass, testName, StringUtils.center(" End Response Content ", 150, PRINT_DELIMITER_REQUEST_PARTS));
    }

    private void printResponseClass(Object response, String testName) {
        Log.info(thisClass, testName, "Response class: " + response.getClass().getName());
    }

    private void printResponseStatusCode(Object response, String testName) throws Exception {
        Log.info(thisClass, testName, "Response (StatusCode): " + WebResponseUtils.getResponseStatusCode(response));
    }

    private void printResponseTitle(Object response, String testName) throws Exception {
        if (WebResponseUtils.getResponseIsHtml(response)) {
            Log.info(thisClass, testName, "Response (Title): " + WebResponseUtils.getResponseTitle(response));
        }
    }

    private void printResponseUrl(Object response, String testName) throws Exception {
        Log.info(thisClass, testName, "Response (Url): " + WebResponseUtils.getResponseUrl(response));
    }

    private void printResponseHeaders(Object response, String testName) throws Exception {
        String[] hs = WebResponseUtils.getResponseHeaderNames(response);
        if (hs != null) {
            for (String h : hs) {
                Log.info(thisClass, testName, "Response (Header): Name: " + h + " Value: " + WebResponseUtils.getResponseHeaderField(response, h));
            }
        }
    }

    private void printResponseMessage(Object response, String testName) throws Exception {
        Log.info(thisClass, testName, "Response (Message): " + WebResponseUtils.getResponseMessage(response));
    }

    private void printResponseText(Object response, String testName) throws Exception {
        Log.info(thisClass, testName, "Response (Full): " + stripBlankLines(WebResponseUtils.getResponseText(response)));
    }

    String stripBlankLines(String text) throws Exception {
        if (text == null || !clearBlankLinesFromResponseFull) {
            return text;
        }
        String[] lines = text.split("[\\r\\n]+");
        StringBuilder strBuilder = new StringBuilder();
        for (String line : lines) {
            if (line != null && line.trim().length() > 0) {
                if (strBuilder.length() > 0) {
                    strBuilder.append("\r\n");
                }
                strBuilder.append(line);
            }
        }
        return strBuilder.toString();
    }

    public void printExpectations(Expectations expectations) throws Exception {
        printExpectations(expectations, null);
    }

    public void printExpectations(Expectations expectations, String[] actions) throws Exception {
        String thisMethod = "printExpectations";
        if (actions != null) {
            Log.info(thisClass, thisMethod, "Actions: " + Arrays.toString(actions));
        }
        if (expectations == null || expectations.getExpectations().isEmpty()) {
            Log.info(thisClass, thisMethod, "Expectations are null. We should never have a test case that has null expectations - that would mean we're NOT validating any results. That's bad, very bad");
            throw new Exception("NO expectations were specified - every test MUST validate its results!!!");
        }
        for (Expectation expected : expectations.getExpectations()) {
            Log.info(thisClass, "printExpectations", "Expectations for test: ");
            if (!isExpectationInActionsList(actions, expected)) {
                logUnusedExpectation(expected);
            }
            printExpectationData(expected);
        }
    }

    boolean isExpectationInActionsList(String[] actions, Expectation expectation) {
        return actions != null && Arrays.asList(actions).contains(expectation.getAction());
    }

    void logUnusedExpectation(Expectation expectation) {
        String thisMethod = "logUnusedExpectation";
        String topAndBottomDelimiter = StringUtils.repeat("*", 94);
        Log.info(thisClass, thisMethod, topAndBottomDelimiter);
        Log.info(thisClass, thisMethod, "* " + StringUtils.rightPad("This expectation will never be processed because its action (" + expectation.getAction() + ")", 90) + " *");
        Log.info(thisClass, thisMethod, "* " + StringUtils.rightPad("is NOT in the list of actions to be performed", 90) + " *");
        Log.info(thisClass, thisMethod, topAndBottomDelimiter);
    }

    void printExpectationData(Expectation expectation) {
        String thisMethod = "logExpectationData";
        if (isExpectationForSuccessfulStatusCode(expectation)) {
            Log.info(thisClass, "printExpectations", "  Action: " + expectation.getAction() + " (expect 200 response)");
        } else {
            Log.info(thisClass, thisMethod, "  Action: " + expectation.getAction());
            Log.info(thisClass, thisMethod, "  Validate against: " + expectation.getSearchLocation());
            Log.info(thisClass, thisMethod, "  How to perform check: " + expectation.getCheckType());
            Log.info(thisClass, thisMethod, "  Key to validate: " + expectation.getValidationKey());
            Log.info(thisClass, thisMethod, "  Value to validate: " + expectation.getValidationValue());
            Log.info(thisClass, thisMethod, "  Print message: " + expectation.getFailureMsg());
        }
    }

    boolean isExpectationForSuccessfulStatusCode(Expectation expectation) {
        return Constants.RESPONSE_STATUS.equals(expectation.getSearchLocation()) && expectation.getValidationValue().equals("200");
    }

    String wrapInDelimiter(String text, String delimiter) {
        return delimiter + " " + text + " " + delimiter;
    }

    void printToLogAndSystemOut(String method, String printString) {
        Log.info(thisClass, method, printString);
        System.out.println(printString);
    }

}
