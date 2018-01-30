/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.security.javaeesec.fat;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@MinimumJavaLevel(javaLevel = 1.8, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)

public class EAREJBModuleTest extends JavaEESecTestBase {
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.ejb.fat");
    protected static Class<?> logClass = EAREJBModuleTest.class;
    protected static String urlBase;
    protected static String TEMP_DIR = "test_temp";
    protected static String EJB_BEAN_JAR_NAME = "SecurityEJBinWAR.jar";
    protected static String EJB_SERVLET_NAME = "SecurityEJBBaseServlet";
    protected static String EJB_WAR_NAME = "ejbinwarservlet.war";
    protected static String EJB_EAR_NAME = "securityejbinwar.ear";
    protected static String EJB_APP_NAME = "securityejbinwar.ear";

    protected DefaultHttpClient httpclient;

    public EAREJBModuleTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        System.out.println("creating war");
        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME, true, EJB_BEAN_JAR_NAME, true, "web.ejb.jar.bean", "web.war.ejb.servlet");

        System.out.println("creating ear");
        // add the servlet war inside the ear
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_NAME, true, EJB_WAR_NAME);

        System.out.println("adding ear");
        //add ear to the server
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_NAME);

        startServer(null, EJB_APP_NAME);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer();
    }

    @Before
    public void setupConnection() {
        // disable auto redirect.
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);

        httpclient = new DefaultHttpClient(httpParams);
    }

    @After
    public void cleanupConnection() throws Exception {
        httpclient.getConnectionManager().shutdown();
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    protected static void startServer(String config, String appName) throws Exception {
        if (config != null)
            myServer.setServerConfigurationFile(config);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(appName);
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testejb_manager__getCallerPrincipal() throws Exception {
        Log.info(logClass, name.getMethodName(), "Entering " + name.getMethodName());

        String queryString = "/securityejbinwar/SimpleServlet?testInstance=ejb02&testMethod=manager";

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "user2", "user2pwd",
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "getCallerPrincipal()=" + "user2");

        Log.info(logClass, name.getMethodName(), "Exiting " + name.getMethodName());
    }

    @Mode(TestMode.LITE)
    @Test
    public void testejb_manager_isUserInRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "Entering " + name.getMethodName());

        String queryString = "/securityejbinwar/SimpleServlet?testInstance=ejb02&testMethod=manager";

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "user2", "user2pwd",
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "getCallerPrincipal()=" + "user2", "isCallerInRole(Manager)=true");

        Log.info(logClass, name.getMethodName(), "Exiting " + name.getMethodName());
    }

    @Mode(TestMode.LITE)
    @Test
    public void testejb_employee_isUserInRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "Entering " + name.getMethodName());

        String queryString = "/securityejbinwar/SimpleServlet?testInstance=ejb02&testMethod=employee";

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "user99", "user99pwd",
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "getCallerPrincipal()=" + "user99", "isCallerInRole(Employee)=true");

        Log.info(logClass, name.getMethodName(), "Exiting " + name.getMethodName());
    }

    @Mode(TestMode.LITE)
    @Test
    public void testejb_employee_group_isUserInRole() throws Exception {
        Log.info(logClass, name.getMethodName(), "Entering " + name.getMethodName());

        String queryString = "/securityejbinwar/SimpleServlet?testInstance=ejb02&testMethod=employee";

        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, "user1", "user1pwd",
                                                          HttpServletResponse.SC_OK);
        verifySecurityContextResponse(response, "getCallerPrincipal()=" + "user", "isCallerInRole(Employee)=true");

        Log.info(logClass, name.getMethodName(), "Exiting " + name.getMethodName());
    }

}
