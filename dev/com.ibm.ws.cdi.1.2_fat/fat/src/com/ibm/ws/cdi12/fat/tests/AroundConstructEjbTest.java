/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.fat.tests;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;

import org.junit.AfterClass;
import org.junit.Test;

import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserException;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.IgnoreTestNamesRule;

public class AroundConstructEjbTest extends AroundConstructTestBase {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.cdi12.fat.tests.AroundConstructTestBase#getServletName()
     */
    @Override
    protected String getServletName() {
        // TODO Auto-generated method stub
        return "ejbTestServlet";
    }

    @Test
    public void testStatelessAroundConstruct() {}

    @Test
    @IgnoreTestNamesRule
    @AllowedFFDC({ "javax.ejb.EJBException", "java.lang.reflect.UndeclaredThrowableException", "java.lang.IllegalStateException" })
    public void testPostConstructErrorMessage() {
        int errMsgCount = 0;
        WebBrowser browser = createWebBrowserForTestCase();

        try {
            WebResponse response = browser.request(createURL("/postConstructErrorMessageApp/errorMessageTestServlet"));
        } catch (MalformedURLException e1) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e1.printStackTrace();
        } catch (WebBrowserException e1) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e1.printStackTrace();
        }
        try {
            errMsgCount = super.server.findStringsInLogs("CWOWB2001E(?=.*POST_CONSTRUCT)(?=.*java.lang.IllegalStateException)").size();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
        }

        assertTrue("The expected error message stating that an interceptor lifecycle callback threw an exception did not appear", errMsgCount > 0);
    }

    private String createURL(String path) throws MalformedURLException {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + path;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWOWB2001E", "CNTR0019E", "SRVE0777E", "SRVE0315E");
        }
    }

}
