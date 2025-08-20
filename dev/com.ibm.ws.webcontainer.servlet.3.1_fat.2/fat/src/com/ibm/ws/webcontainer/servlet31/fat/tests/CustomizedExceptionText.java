package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class CustomizedExceptionText {

    private static final Logger LOGGER = Logger.getLogger(CustomizedExceptionText.class.getName());
    private static final String CUSTOM_ERROR_APP_NAME = "CustomErrorTestApp";

    @Server("servlet31_customizedExceptionText")
<<<<<<< HEAD
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive customErrorApp = ShrinkHelper.buildDefaultApp(
            CUSTOM_ERROR_APP_NAME + ".war",
            "com.ibm.ws.webcontainer.servlet_31_fat.customerrortest.war.test.servlets"
        );
        customErrorApp = (WebArchive) ShrinkHelper.addDirectory(
            customErrorApp,
            "test-applications/" + CUSTOM_ERROR_APP_NAME + ".war/resources"
        );
        ShrinkHelper.exportDropinAppToServer(server, customErrorApp);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
    /**
     * Verify that the customized exception text configured in server.xml
     * is returned instead of the default
     * "SRVE0218E: Forbidden" message
     * when the application sends a 403 Forbidden error.
     */
    @Test
    public void tes403CustomMessage() throws Throwable {
        server.stopServer();
        server.setServerConfigurationFile("customizedExceptionTest/server-test-SRVE0218E-custom-message.xml");
        server.startServer();
        server.waitForStringInLogUsingMark("CWWKT0016I.*" + CUSTOM_ERROR_APP_NAME + ".*");

        String requestURL = baseURL() + "/" + CUSTOM_ERROR_APP_NAME + "/forbidden";
        String result = checkRequest(requestURL, 403, "Custom 403 Forbidden error message", "SRVE0218E");

        assertTrue("Custom 403 text not found in response:\n" + result, result.contains("PASS"));
    }

    /**
     * Verify that the customized exception text configured in server.xml
     * is returned instead of the default
     * "SRVE0232E: An exception occurred" message
     * when a servlet triggers a 500 Internal Server Error.
     */
    @Test
    public void test500CustomMessage() throws Throwable {
        server.stopServer();
        server.setServerConfigurationFile("customizedExceptionTest/server-test-SRVE0232E-custom-message.xml");
        server.startServer();
        server.waitForStringInLogUsingMark("CWWKT0016I.*" + CUSTOM_ERROR_APP_NAME + ".*");

        String requestURL = baseURL() + "/" + CUSTOM_ERROR_APP_NAME + "/error";
        String result = checkRequest(requestURL, 500, "Custom 500 internal server error message", "SRVE0232E");

        assertTrue("Custom 500 text not found in response:\n" + result, result.contains("PASS"));
    }

    private String checkRequest(String URL, int responseCode, String expectedResText, String notExpectedResText) throws Throwable {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);
        WebResponse resp = wc.getResponse(URL);

        int code = resp.getResponseCode();
        String body = resp.getText();

        StringBuilder result = new StringBuilder();
        result.append("Checking Response Code: Expected [" + responseCode + "], actual [" + code + "]. ");
        result.append(code == responseCode ? "PASS\n" : "FAIL\n");

        if (!expectedResText.equals("SKIP")) {
            result.append("Checking Expected Response text: ");
            result.append(body.contains(expectedResText) ? "PASS\n" : "FAIL\n");
        }

        if (!notExpectedResText.equals("SKIP")) {
            result.append("Checking NOT Expected Response text: ");
            result.append(body.contains(notExpectedResText) ? "FAIL\n" : "PASS\n");
        }

        LOGGER.log(Level.INFO, "checkRequest Body:\n" + body);
        return result.toString();
    }

    private String baseURL() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
    }
}

package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class CustomizedExceptionText {

    private static final Logger LOGGER = Logger.getLogger(CustomizedExceptionText.class.getName());
    private static final String CUSTOM_ERROR_APP_NAME = "CustomErrorTestApp";

    @Server("servelet31_customizedExceptionText")
=======
>>>>>>> d5617bf6dbd (add tests)
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive customErrorApp = ShrinkHelper.buildDefaultApp(
            CUSTOM_ERROR_APP_NAME + ".war",
            "com.ibm.ws.webcontainer.servlet_31_fat.customerrortest.war.test.servlets"
        );
        customErrorApp = (WebArchive) ShrinkHelper.addDirectory(
            customErrorApp,
            "test-applications/" + CUSTOM_ERROR_APP_NAME + ".war/resources"
        );
        ShrinkHelper.exportDropinAppToServer(server, customErrorApp);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }
    /**
     * Verify that the customized exception text configured in server.xml
     * is returned instead of the default
     * "SRVE0218E: Forbidden" message
     * when the application sends a 403 Forbidden error.
     */
    @Test
    public void tes403CustomMessage() throws Throwable {
        server.stopServer();
        server.setServerConfigurationFile("customizedExceptionTest/server-test-SRVE0218E-custom-message.xml");
        server.startServer();
        server.waitForStringInLogUsingMark("CWWKT0016I.*" + CUSTOM_ERROR_APP_NAME + ".*");

        String requestURL = baseURL() + "/" + CUSTOM_ERROR_APP_NAME + "/forbidden";
        String result = checkRequest(requestURL, 403, "Custom 403 Forbidden error message", "SRVE0218E");

        assertTrue("Custom 403 text not found in response:\n" + result, result.contains("PASS"));
    }

    /**
     * Verify that the customized exception text configured in server.xml
     * is returned instead of the default
     * "SRVE0232E: An exception occurred" message
     * when a servlet triggers a 500 Internal Server Error.
     */
    @Test
    public void test500CustomMessage() throws Throwable {
        server.stopServer();
        server.setServerConfigurationFile("customizedExceptionTest/server-test-SRVE0232E-custom-message.xml");
        server.startServer();
        server.waitForStringInLogUsingMark("CWWKT0016I.*" + CUSTOM_ERROR_APP_NAME + ".*");

        String requestURL = baseURL() + "/" + CUSTOM_ERROR_APP_NAME + "/error";
        String result = checkRequest(requestURL, 500, "Custom 500 internal server error message", "SRVE0232E");

        assertTrue("Custom 500 text not found in response:\n" + result, result.contains("PASS"));
    }

    private String checkRequest(String URL, int responseCode, String expectedResText, String notExpectedResText) throws Throwable {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);
        WebResponse resp = wc.getResponse(URL);

        int code = resp.getResponseCode();
        String body = resp.getText();

        StringBuilder result = new StringBuilder();
        result.append("Checking Response Code: Expected [" + responseCode + "], actual [" + code + "]. ");
        result.append(code == responseCode ? "PASS\n" : "FAIL\n");

        if (!expectedResText.equals("SKIP")) {
            result.append("Checking Expected Response text: ");
            result.append(body.contains(expectedResText) ? "PASS\n" : "FAIL\n");
        }

        if (!notExpectedResText.equals("SKIP")) {
            result.append("Checking NOT Expected Response text: ");
            result.append(body.contains(notExpectedResText) ? "FAIL\n" : "PASS\n");
        }

        LOGGER.log(Level.INFO, "checkRequest Body:\n" + body);
        return result.toString();
    }

    private String baseURL() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
    }
}
