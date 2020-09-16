/**
 *
 */
package com.ibm.ws.fat.grpc;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.net.URL;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.testapp.g3store.utilsConsumer.ConsumerUtils;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

/**
 *
 */
public class StoreClientTestsUtils {

    protected static final Class<?> c = StoreClientTestsUtils.class;

    public static Object[] readAppNames(Object... values) {
        Object[] arr = new Object[values.length];
        int i = 0;
        for (Object v : values) {
            arr[i++] = v;
        }
        return arr;
    }

    public static HtmlPage getProducerResultPage(WebClient webClient, String inputTestName, LibertyServer server) throws Exception {

        HtmlForm form = assertConnectProducerServer(server, webClient, inputTestName);

        if (form != null) {
            // set the testName
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("testName");
            inputText.setValueAttribute(inputTestName);

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            return submitButton.click();
        } else {
            return null;
        }

    }

    public static HtmlPage getProducerResultPage(WebClient webClient, String inputTestName, String inputAppName, LibertyServer server) throws Exception {

        HtmlForm form = assertConnectProducerServer(server, webClient, inputTestName);
        if (form != null) {

            // set the testName
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("testName");
            inputText.setValueAttribute(inputTestName);

            // set the appName
            HtmlTextInput inputApptext = (HtmlTextInput) form.getInputByName("appName");
            inputApptext.setValueAttribute(inputAppName);

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            return submitButton.click();
        } else {
            return null;
        }
    }

    public static HtmlPage getConsumerResultPage(WebClient webClient, String inputTestName, LibertyServer server) throws Exception {

        HtmlForm form = assertConnectConsumerServer(server, webClient, false, inputTestName);
        if (form != null) {
            // set a name, which we'll expect the RPC to return
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("testName");

            inputText.setValueAttribute(inputTestName);

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            return submitButton.click();
        } else {
            return null;
        }

    }

    public static HtmlPage getConsumerResultPage(WebClient webClient, String inputTestName, String inputAppName, boolean addAuthHeader, LibertyServer server) throws Exception {

        HtmlForm form = assertConnectConsumerServer(server, webClient, addAuthHeader, inputTestName);
        if (form != null) {
            // set a name, which we'll expect the RPC to return
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("testName");
            inputText.setValueAttribute(inputTestName);

            // set the appName
            HtmlTextInput inputApptext = (HtmlTextInput) form.getInputByName("appName");
            inputApptext.setValueAttribute(inputAppName);

            // submit, and execute the RPC
            HtmlSubmitInput submitButton = form.getInputByName("submit");
            return submitButton.click();
        } else {
            return null;
        }
    }

    public static HtmlForm assertConnectProducerServer(LibertyServer server, WebClient webClient, String inputTestName) throws Exception {

        Log.info(c, inputTestName, inputTestName);
        // Construct the URL for the test
        URL url = GrpcTestUtils.createHttpUrl(server, "StoreProducerApp", "ProducerServlet");
        Log.info(c, inputTestName, " ------URL=[" + url + "]");
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        // Log the page for debugging if necessary in the future.
        Log.info(c, inputTestName, page.asText());
        //Log.info(c, name.getMethodName(), page.asXml());

        assertTrue("the servlet was not loaded correctly",
                   page.asText().contains("gRPC Store Producer client"));

        return page.getFormByName("form1");
    }

    public static HtmlForm assertConnectConsumerServer(LibertyServer server, WebClient webClient, boolean addAuthHeader, String inputTestName) throws Exception {

        Log.info(c, inputTestName, inputTestName);
        // Construct the URL for the test
        URL url = GrpcTestUtils.createHttpUrl(server, "StoreConsumerApp", "ConsumerServlet");
        Log.info(c, inputTestName, " ------URL=[" + url + "]");
        if (addAuthHeader) {
            webClient.addRequestHeader("Authorization", getAuthValue(inputTestName));
        }

        HtmlPage page = (HtmlPage) webClient.getPage(url);

        // Log the page for debugging if necessary in the future.
        Log.info(c, inputTestName, page.asText());
        //Log.info(c, name.getMethodName(), page.asXml());

        assertTrue("the servlet was not loaded correctly",
                   page.asText().contains("gRPC Store Consumer client"));

        return page.getFormByName("form1");
    }

    public static String getAuthValue(String inputTestName) throws UnsupportedEncodingException {
        Log.info(c, inputTestName, ConsumerUtils.createBasicAuthHeaderValue("dev", "hello"));
        return ConsumerUtils.createBasicAuthHeaderValue("dev", "hello");
    }

    public static void createAssertMultiApps(String testname, LibertyServer server) throws Exception {

        WebClient webClient = new WebClient();
        String appName1 = "myApp1";
        String appName2 = "myApp2";
        String appName3 = "myApp3";
        String appName4 = "myApp4";
        try {
            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname,
                     " ----- invoking producer servlet client to create apps: +[" + appName1 + "]" + " [" + appName2 + "]" + " [" + appName3 + "]" + " [" + appName4 + "]");

            HtmlPage page = StoreClientTestsUtils.getProducerResultPage(webClient, "createMultiApps", server);

            if (page != null) {
                String response = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, testname, response);

                // but there is no order since it is server streaming, assert them individually

                assertTrue(response.contains("Store has successfully added the app [" + appName1 + "]"));
                assertTrue(response.contains("Store has successfully added the app [" + appName2 + "]"));
                assertTrue(response.contains("Store has successfully added the app [" + appName3 + "]"));
                assertTrue(response.contains("Store has successfully added the app [" + appName4 + "]"));

            } else {
                fail("testCreate: no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
        } finally {
            webClient.close();
            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname,
                     " ----- completed producer servlet client to create apps: +[" + appName1 + "]" + " [" + appName2 + "]" + " [" + appName3 + "]" + " [" + appName4 + "]");
            Log.info(c, testname, " ------------------------------------------------------------");

        }
    }

    public static void deleteAssertMultiApps(String testname, LibertyServer server) throws Exception {

        WebClient webClient = new WebClient();
        String appName1 = "myApp1";
        String appName2 = "myApp2";
        String appName3 = "myApp3";
        String appName4 = "myApp4";
        try {

            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname,
                     " ----- invoking producer servlet client to delete apps: +[" + appName1 + "]" + " [" + appName2 + "]" + " [" + appName3 + "]" + " [" + appName4 + "]");

            HtmlPage page = StoreClientTestsUtils.getProducerResultPage(webClient, "deleteMultiApps", server);

            if (page != null) {
                String response = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, testname, response);

                assertTrue(response.contains("The app [" + appName1 + "] has been removed from the Store"));
                assertTrue(response.contains("The app [" + appName2 + "] has been removed from the Store"));
                assertTrue(response.contains("The app [" + appName3 + "] has been removed from the Store"));
                assertTrue(response.contains("The app [" + appName4 + "] has been removed from the Store"));

            } else {
                fail("testCreate: no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            throw e;
        } finally {
            webClient.close();
            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname,
                     " ----- completed producer servlet client to delete apps: +[" + appName1 + "]" + " [" + appName2 + "]" + " [" + appName3 + "]" + " [" + appName4 + "]");
            Log.info(c, testname, " ------------------------------------------------------------");
        }

    }

    public static void createAssertMyApp(String appName, String testname, LibertyServer server) throws Exception {

        WebClient webClient = new WebClient();
        try {

            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname, " ----- invoking producer servlet client to create app: [" + appName + "]");

            HtmlPage page = StoreClientTestsUtils.getProducerResultPage(webClient, "createApp", appName, server);

            if (page != null) {
                String response = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, testname, response);

                boolean isValidResponse = response.contains("createResult");

                assertTrue(isValidResponse);
            } else {
                fail("testCreate: no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
        } finally {
            webClient.close();
            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname, " ----- completed producer servlet client to create app: [" + appName + "] ----- ");
            Log.info(c, testname, " ------------------------------------------------------------");

        }
    }

    public static void deleteAssertMyApp(String appName, String testname, LibertyServer server) throws Exception {

        WebClient webClient = new WebClient();
        try {

            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname, " ----- invoking producer servlet client to delete app: [" + appName + "] ----- ");

            HtmlPage page = StoreClientTestsUtils.getProducerResultPage(webClient, "deleteApp", appName, server);

            if (page != null) {
                String response = page.asText();
                // Log the page for debugging if necessary in the future.
                Log.info(c, testname, response);

                boolean isValidResponse = response.contains("The app [" + appName + "] has been removed from the server");

                assertTrue(isValidResponse);
            } else {
                fail("testCreate: no response from the grpc server"); // should not happen
            }

        } catch (Exception e) {
            e.getMessage();
            throw e;
        } finally {
            webClient.close();
            Log.info(c, testname, " ------------------------------------------------------------");
            Log.info(c, testname, " ----- completed producer servlet client to delete app: [" + appName + "] ----- ");
            Log.info(c, testname, " ------------------------------------------------------------");
        }

    }

}
