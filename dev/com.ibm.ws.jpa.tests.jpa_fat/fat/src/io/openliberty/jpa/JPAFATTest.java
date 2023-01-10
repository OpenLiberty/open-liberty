/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlets;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

public class JPAFATTest extends FATServletClient {
    private static final String CLASS_NAME = JPAFATTest.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);
    private static final int CONN_TIMEOUT = 5;
    private static final String[] APP_NAMES = { "acme", "managedApp", "loose", "validation", "unmanaged", "injected" };
    private static final String[] ALLOWED_MESSAGES = { "SRVE9967W", // The manifest class path derbyLocale.* can not be found
                                                       "CWWJP0009E", // The server cannot create an EntityManagerFactory factory for the TestProvider persistent unit from the provider.TestProvider provider
                                                       "CWWJP0013E", // The server cannot locate the a/datasource/which/does/not/exist data source
                                                       "CWWJP9991W" // openjpa.Enhance: Warn: Detected the following possible violations of the restrictions placed on property access persistent types
    };

    @Rule
    public TestName testName = new TestName();

    @Server("com.ibm.ws.jpa.fat.server")
    @TestServlets({
//                    @TestServlet(servlet = Spec21DDSServlet.class, path = appPath)
    })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive acmeApp = ShrinkWrap.create(WebArchive.class, "acme.war")//
                        .addPackage("acme")
                        .addPackage("data")
                        .addPackage("web");
        ShrinkHelper.addDirectory(acmeApp, "test-applications/acme/resources/");
        ShrinkHelper.exportToServer(server, "apps", acmeApp);

        WebArchive mgdManualEnhApp = ShrinkWrap.create(WebArchive.class, "managedmanualenhancement.war")//
                        .addPackage("acme")
                        .addPackage("data")
                        .addPackage("web");
        ShrinkHelper.addDirectory(mgdManualEnhApp, "test-applications/managedmanualenhancement/resources/");
        ShrinkHelper.exportToServer(server, "apps", mgdManualEnhApp);

        for (String appName : APP_NAMES)
            server.addInstalledAppForValidation(appName);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(ALLOWED_MESSAGES);
    }

    private static final String A_PERSISTENCE_PROVIDER_IS_PRESENT_AND_WORKING = "A persistence provider is present and working";
    public static final String THIS_IS_JPA_SERVLET = "This is JpaServlet.";
    private static final String UNMANAGED_APP = "unmanaged";

    protected BufferedReader getConnectionStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        return br;
    }

    protected HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
//        Log.info(AbstractJPAFATTest.class, "getHttpConnection", url.toString());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        return con;
    }

    String callTestServlet(LibertyServer server, String app, String servlet, String test) throws Exception {
        return callTestServlet(server, app, servlet, test, (String[]) null);
    }

    String callTestServlet(LibertyServer server, String app, String servlet, String test, String... parms) throws Exception {
        StringBuffer urlString = new StringBuffer("http://");
        urlString.append(server.getHostname());
        urlString.append(":");
        urlString.append(server.getHttpDefaultPort());
        urlString.append("/");
        urlString.append(app);
        urlString.append("/");
        urlString.append(servlet);
        urlString.append("?");
        urlString.append("testMethod=");
        urlString.append(test);
        if (parms != null && parms.length > 0) {
            for (String parm : parms) {
                urlString.append("&");
                urlString.append(parm);
            }
        }

        URL url = new URL(urlString.toString());
        svLogger.info("AbstractJPAFATTEST." + test + " : Calling " + servlet + " servlet with URL : " + url.toString());

        StringBuilder lines = new StringBuilder();
        HttpURLConnection con = null;
        try {
            con = getHttpConnection(url);
            BufferedReader br = getConnectionStream(con);

            String sep = System.getProperty("line.separator");
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                svLogger.info(" <<-- " + line);
                lines.append(line).append(sep);
            }
        } catch (Exception ex) {
            svLogger.logp(Level.SEVERE, CLASS_NAME, "callTestServlet", "Unexpected exception:", ex);
            throw ex;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

        return lines.toString();
    }

    @Test
    public void testCallUnmanagedJpaServletWithOpenJpaPersistenceProvider() throws IOException {
        svLogger.entering(CLASS_NAME, "testCallUnmanagedJpaServletWithOpenJpaPersistenceProvider");

        String host = server.getMachine().getHostname();
        URL url = new URL("http://" + host + ":" + server.getHttpDefaultPort() + "/acme?useIBMPersistenceProvider=false");
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);

        // With the WAS jars we expect a "com.ibm.ws.persistence.EntityManagerImpl",
        // rather than a "org.apache.openjpa.persistence.EntityManagerImpl",
        // even though we're using the OpenJPA persistence provider. That
        // persistence provider then seems to use a WAS broker which gives us the
        // WAS entity manager
        validateServletOutput(br, "com.ibm.ws.persistence.EntityManagerImpl");

        con.disconnect();
        svLogger.exiting(CLASS_NAME, "testCallUnmanagedJpaServletWithOpenJpaPersistenceProvider");
    }

    @Test
    public void testCallUnmanagedJpaServletWithWebSpherePersistenceProvider() throws IOException {
        String host = server.getMachine().getHostname();
        URL url = new URL("http://" + host + ":" + server.getHttpDefaultPort() + "/acme?useIBMPersistenceProvider=true");
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);

        validateServletOutput(br, "com.ibm.ws.persistence.EntityManagerImpl");

        con.disconnect();
    }

    @Test
    public void testSlightlyApplicationManagedJPAServlet() throws ProtocolException, IOException {
        String host = server.getMachine().getHostname();
        URL url = new URL("http://" + host + ":" + server.getHttpDefaultPort() + "/managedmanualenhancement/constructed");
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);

        validateServletOutput(br, "com.ibm.ws.persistence.EntityManagerImpl");

        con.disconnect();

    }

    @Test
    public void testJNDIApplicationManagedJPAServlet() throws ProtocolException, IOException {
        String host = server.getMachine().getHostname();
        URL url = new URL("http://" + host + ":" + server.getHttpDefaultPort() + "/managedmanualenhancement/applicationmanaged/jndi?manualenhancement=true");
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);

        String expectedEntityManager = "com.ibm.ws.persistence.EntityManagerImpl";
        validateServletOutput(br, expectedEntityManager);

        con.disconnect();

    }

    @Test
    public void testJNDIApplicationManagedJPAServletWithNoPreenhancement() throws ProtocolException, IOException {
        String host = server.getMachine().getHostname();
        URL url = new URL("http://" + host + ":" + server.getHttpDefaultPort() + "/managed/applicationmanaged/jndi");
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);

        String expectedEntityManager = "com.ibm.ws.persistence.EntityManagerImpl";
        validateServletOutput(br, expectedEntityManager);

        con.disconnect();

    }

    /**
     * A negative test which checks that if we use a datasource which
     * doesn't exist, we get an exception complaining that the
     * datasource doesn't exist. (This shows we were paying attention
     * to the persistence.xml.)
     *
     * @throws ProtocolException
     * @throws IOException
     */
    @Test
    public void testJNDIApplicationManagedWithMisconfiguredDataSourceJPAServlet() throws ProtocolException, IOException {
        svLogger.info("JPAFATTest.testJNDIApplicationManagedWithMisconfiguredDataSourceJPAServlet: BJK");
        Log.info(JPAFATTest.class, "testJNDIApplicationManagedWithMisconfiguredDataSourceJPAServlet", "BJK");
        String host = server.getMachine().getHostname();
        URL url = new URL("http://" + host + ":" + server.getHttpDefaultPort() + "/managedmanualenhancement/applicationmanaged/jndiwithunconfigureddatasource");
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);

        // We expect this to fail because the datasource isn't there
        // (if it passed it means we ignored persistence.xml)
        String line = null;
        line = br.readLine();
        assertEquals(THIS_IS_JPA_SERVLET, line);
        boolean gotDataSourceError = false;
        String otherError = null;
        while (line != null) {
            svLogger.info("JPAFATTest.testJNDIApplicationManagedWithMisconfiguredDataSourceJPAServlet : LINE: " + line);
            if (line.contains("PersistenceException")) {
                gotDataSourceError = true;
                assertTrue(line.contains("org.apache.openjpa.util.UserException"));
            }
            if (!gotDataSourceError && line.contains("ERROR")) {
                otherError = line;
            }
            line = br.readLine();

        }

        assertTrue("A persistence unit configured to use a datasource which doesn't exist worked fine; the persistence.xml seems to have been ignored.", gotDataSourceError);

        assertNull("A persistence unit configured to use a datasource which doesn't exist hit a problem but it wasn't the one we were hoping for:" + otherError, otherError);

        con.disconnect();

    }

    @Test
    public void testJNDIContainerManagedJPAServletLTC() throws ProtocolException, IOException {
        exerciseJNDIContainerManagedJPAServletLTC("/managedmanualenhancement/containermanaged/jndi?action=create&manualenhancement=true&useLTC=true");
    }

    @Test
    public void testJNDIContainerManagedJPAServlet() throws ProtocolException, IOException {
        exerciseJNDIContainerManagedJPAServlet("/managedmanualenhancement/containermanaged/jndi?action=create&manualenhancement=true&useLTC=false",
                                               "/managedmanualenhancement/containermanaged/jndi?action=retrieve&manualenhancement=true&useLTC=false");
    }

    /*
     * Verifies simple C,R operations for orm.xml defined entity.
     */
    @Test
    public void testJNDIContainerManagedJPAServletXML() throws ProtocolException, IOException {
        exerciseJNDIContainerManagedJPAServlet("/managed/containermanaged/jndi?action=xcreate&manualenhancement=false&useLTC=false",
                                               "/managed/containermanaged/jndi?action=xretrieve&manualenhancement=false&useLTC=false");
    }

    @Test
    public void testJNDIContainerManagedJPAServletWithNoPreEnhancementLTC() throws ProtocolException, IOException {
        exerciseJNDIContainerManagedJPAServletLTC("/managed/containermanaged/jndi?action=create&useLTC=true");
    }

    @Test
    public void testJNDIContainerManagedJPAServletWithNoPreEnhancement() throws ProtocolException, IOException {
        exerciseJNDIContainerManagedJPAServlet("/managed/containermanaged/jndi?action=create&useLTC=false",
                                               "/managed/containermanaged/jndi?action=retrieve&useLTC=false");
    }

    @Test
    public void testLooselyConfiguredJPAServletWithLTC() throws ProtocolException, IOException {
        svLogger.entering(CLASS_NAME, "testLooselyConfiguredJPAServletWithLTC");
        exerciseJNDIContainerManagedJPAServletLTC("/loose/containermanaged/jndi?action=create&useLTC=true");
        svLogger.exiting(CLASS_NAME, "testLooselyConfiguredJPAServletWithLTC");
    }

    @Test
    public void testLooselyConfiguredJPAServlet() throws ProtocolException, IOException {
        svLogger.entering(CLASS_NAME, "testLooselyConfiguredJPAServlet");
        exerciseJNDIContainerManagedJPAServlet("/loose/containermanaged/jndi?action=create&useLTC=false",
                                               "/loose/containermanaged/jndi?action=retrieve&useLTC=false");
        svLogger.exiting(CLASS_NAME, "testLooselyConfiguredJPAServlet");
    }

    @Test
    public void testInjectedContainerManagedJPAServlet() throws ProtocolException, IOException {
        exerciseJNDIContainerManagedJPAServlet("/injected/persistencecontext?action=create&useLTC=false",
                                               "/injected/persistencecontext?action=retrieve&useLTC=false");
    }

    @Test
    public void testInjectedApplicationManagedJPAServlet() throws ProtocolException, IOException {
        exerciseJNDIContainerManagedJPAServlet("/injected/persistenceunit?action=create&useLTC=false",
                                               "/injected/persistenceunit?action=retrieve&useLTC=false");
    }

    @Test
    public void testInjectedContainerManagedJPAServletLTC() throws ProtocolException, IOException {
        exerciseJNDIContainerManagedJPAServlet("/injected/persistencecontext?action=create&useLTC=false",
                                               "/injected/persistencecontext?action=retrieve&useLTC=true");
    }

    @Test
    public void testInjectedApplicationManagedJPAServletLTC() throws ProtocolException, IOException {
        exerciseJNDIContainerManagedJPAServlet("/injected/persistenceunit?action=create&useLTC=false",
                                               "/injected/persistenceunit?action=retrieve&useLTC=true");
    }

    @Test
    public void testEMClosureWithNonGlobalTransaction() throws ProtocolException, IOException {
        //create entity using global transaction
        BufferedReader br = exerciseEMClosureTestServlet(getEMClosureTestURLByAction("create"));
        verifyTestServletOutput(br, "Created Company with id", true, "Test entity is NOT created successfully");
        //find entity using non-global transaction, EM should be closed when the transaction is done.
        br = exerciseEMClosureTestServlet(getEMClosureTestURLByAction("find"));
        verifyTestServletOutput(br, "Found Company : ", true, "Test entity is NOT found");
        //merge entity using global transaction
        br = exerciseEMClosureTestServlet(getEMClosureTestURLByAction("merge"));
        verifyTestServletOutput(br, "EntityExistsException", false,
                                "Entity Manager is not properly closed when the non-global transaction finished. Existing entity is not detached from previous context.");
    }

    /**
     * @param br
     * @throws IOException
     */
    private void verifyTestServletOutput(BufferedReader br, String messageToFind, boolean meanToBeFound, String failureMessage) throws IOException {
        boolean found = false;
        String line = br.readLine();
        while (line != null) {
            int index = line.indexOf(messageToFind);
            if (index >= 0) {
                if (!meanToBeFound) {
                    fail(failureMessage);
                }
                found = true;
                break;
            }
            line = br.readLine();
        }
        if (!found && meanToBeFound) {
            fail(failureMessage);
        }
    }

    private BufferedReader exerciseEMClosureTestServlet(URL url) throws ProtocolException, IOException {
        svLogger.info("JPAFATTest.EMClosureTest : " + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        return getConnectionStream(con);
    }

    private URL getEMClosureTestURLByAction(String action) throws MalformedURLException {
        String host = server.getMachine().getHostname();
        return new URL("http://" + host + ":" + server.getHttpDefaultPort() + "/managed/containermanaged/EMClosureTest?action=" + action);
    }

    private void exerciseJNDIContainerManagedJPAServlet(String createURL, String retrieveURL) throws ProtocolException, IOException {
        String host = server.getMachine().getHostname();
        URL createUrl = new URL("http://" + host + ":" + server.getHttpDefaultPort() + createURL);
        URL retrieveUrl = new URL("http://" + host + ":" + server.getHttpDefaultPort() + retrieveURL);

        final String expectedEntityManager = "com.ibm.ws.jpa.management.JPATxEntityManager";

        // Go through a few cycles of creating and retrieving, making sure the count of things in the database increments each time
        // Do it in separate requests so that we're not all in the same transaction (hopefully)
        HttpURLConnection con = getHttpConnection(retrieveUrl);
        BufferedReader br = getConnectionStream(con);
        Set<Integer> ids = validateRetrieveServletOutput(br, expectedEntityManager);

        // d140569: getting rid of loop, as it effectively negates any test execution after the first 10.
        int i = ids.size();
        con = getHttpConnection(createUrl);
        br = getConnectionStream(con);

        validateCreateServletOutput(br, ids, expectedEntityManager);
        con = getHttpConnection(retrieveUrl);
        br = getConnectionStream(con);
        int itemCount = validateRetrieveServletOutput(br, expectedEntityManager).size();
        assertEquals("The number of items in the database was wrong", i + 1, itemCount);

        con.disconnect();

    }

    private void exerciseJNDIContainerManagedJPAServletLTC(String createURL) throws ProtocolException, IOException {

        String host = server.getMachine().getHostname();
        URL createUrl = new URL("http://" + host + ":" + server.getHttpDefaultPort() + createURL);

        HttpURLConnection con = getHttpConnection(createUrl);
        BufferedReader br = getConnectionStream(con);

        String line = br.readLine();

        while (line != null) {
            if (line.contains("javax.persistence.TransactionRequiredException")) {
                return;
            }
            line = br.readLine();
        }

        assertFalse("The test should have thrown a javax.persistence.TransactionRequiredException during persist", true);
    }

    /*
     * Verifies that JPA functions properly and picks up updates after a runtime application update
     * occurs. The variation:
     * 1) OpenJPA data cache disabled by default in persistence.xml.
     * 2) verifies data caching is disabled for the PU.
     * 3) updates the persistence.xml to enable data caching for the PU.
     * 4) verifies the app auto-restart
     * 5) verifies data caching is enabled for the PU.
     */
    @Test
    public void testApplicationUpdate() throws Exception {
        final String method = "testApplicationUpdate";
        boolean success = invokeLooseVerification(method, false);
        if (!success) {
            // If the normal invoke failed, don't bother trying the app update
            return;
        }

        // Update persistence.xml and make sure the app still works
        server.copyFileToLibertyServerRoot("loose-files/WEB-INF/classes/META-INF", "looseUpdate/persistence.xml");
        // Wait for message to indicate application updated...
        // Occasionally there are two of these messages - if there are we need the second one
        // I'm not sure why we'd ever need to wait for two messages. Dropping this down to wait for only 1 message
        // to speed this bucket up by ~ 120 seconds. If we start seeing build failures because of a missing message we'll
        // need to do something smarter here.
        int messageCount = server.waitForMultipleStringsInLog(1, "CWWKZ0003I.*loose");
        assertTrue("Application loose does not appear to have restarted after update.", messageCount != 0);
        invokeLooseVerification(method, true);
    }

    /**
     * @param method
     * @return
     * @throws MalformedURLException
     * @throws IOException
     * @throws ProtocolException
     */
    private boolean invokeLooseVerification(final String method, boolean changed) throws MalformedURLException, IOException, ProtocolException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/loose/containermanaged/jndi?action=verify&useLTC=false&appChanged=" + changed);
        svLogger.info("JPAFATTest." + method + " : Calling test Application with URL=" + url.toString());
        HttpURLConnection con = HttpUtils.getHttpConnection(url, HttpURLConnection.HTTP_OK, CONN_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);

        String line = null;
        boolean success = false;
        while ((line = br.readLine()) != null) {
            svLogger.info("JPAFATTest." + method + " : LINE: " + line);
            assertFalse("Invocation contains error", line.contains("ERROR"));
            if (line.contains("SUCCESS")) {
                success = true;
            }
        }
        assertTrue("Invocation was successful", success);
        return success;
    }

    /**
     * Tests the provider resolver. Verifies that the two providers registered as OSGi bundles are
     * available in addition to the dummy provider available on the app classpath.
     */
    @Test
    public void testResolver() throws Exception {
        final String testMethod = "testResolver";
        executeTest(UNMANAGED_APP, UNMANAGED_APP, testMethod);
    }

    /**
     * This test verifies that the WsJpa provider
     * is used when the provider is specified in the persistence.xml
     */
    @Test
    public void testWsJPAProviderPersistenceXML() throws Exception {
        final String testMethod = "testWsJPAProviderPersistenceXML";
        executeTest(UNMANAGED_APP, UNMANAGED_APP, testMethod);
    }

    /**
     * This test verifies that the WsJpa provider (available in a bundle)
     * is returned when the javax.persistence.provider property is used.
     */
    @Test
    public void testWsJPAProviderPersistenceProp() throws Exception {
        final String testMethod = "testWsJPAProviderPersistenceProp";
        executeTest(UNMANAGED_APP, UNMANAGED_APP, testMethod);
    }

    /**
     * This test verifies that the OpenJPA provider
     * is used when the provider is specified in the persistence.xml
     */
    @Test
    public void testOpenJPAProviderPersistenceProp() throws Exception {
        final String testMethod = "testOpenJPAProviderPersistenceProp";
        executeTest(UNMANAGED_APP, UNMANAGED_APP, testMethod);
    }

    /**
     * This test verifies that the OpenJPA provider (available in a bundle)
     * is returned when the javax.persistence.provider property is used.
     */
    @Test
    public void testOpenJPAProviderPersistenceXML() throws Exception {
        final String testMethod = "testOpenJPAProviderPersistenceXML";
        executeTest(UNMANAGED_APP, UNMANAGED_APP, testMethod);
    }

    /**
     * This test verifies that the dummy provider (available on app classpath)
     * is used when the provider is specified in the persistence.xml
     */
    @Test
    public void testDummyJPAProviderPersistenceXML() throws Exception {
        final String testMethod = "testDummyJPAProviderPersistenceXML";
        executeTest(UNMANAGED_APP, UNMANAGED_APP, testMethod);
    }

    /**
     * This test verifies that the dummy provider (available on app classpath)
     * is used when the javax.persistence.provider property is used.
     */
    @Test
    public void testDummyJPAProviderPersistenceProp() throws Exception {
        final String testMethod = "testDummyJPAProviderPersistenceProp";
        executeTest(UNMANAGED_APP, UNMANAGED_APP, testMethod);
    }

    /**
     * This test verifies that an pu def without a provider specified returns a valid
     * EMF.
     */
    @Test
    public void testDefaultJPAProvider() throws Exception {
        final String testMethod = "testDefaultJPAProvider";
        executeTest(UNMANAGED_APP, UNMANAGED_APP, testMethod);
    }

    /**
     * This test verifies the common usage pattern of using an unmanaged/JSE bootstrapped
     * EMF with JTA transactions.
     */
    @Test
    public void testUnmanagedJTA() throws Exception {
        final String testMethod = "testUnmanagedJTA";
        executeTest(UNMANAGED_APP, UNMANAGED_APP, testMethod);
    }

    /**
     * Variety pack: Verifies usage of a non-JTA data source with RESOURCE_LOCAL transactions
     * using an entity defined within a non-default mapping file (orm_2.xml).
     *
     * @throws Exception
     */
    @Test
    public void testUnmanagedNonJTAXML() throws Exception {
        final String testMethod = "testUnmanagedNonJTAXML";
        executeTest(UNMANAGED_APP, UNMANAGED_APP, testMethod);
    }

    /**
     * This test verifies that specifying an unknown provider does not
     * return an EMF when specified as the javax.persistence.provider property.
     */
    @Test
    public void testUnknownProviderPersistenceProp() throws Exception {
        final String testMethod = "testUnknownProviderPersistenceProp";
        executeTest(UNMANAGED_APP, UNMANAGED_APP, testMethod);
    }

    /**
     * This test verifies that specifying an unknown provider does not
     * return an EMF when specified as the javax.persistence.provider property.
     */
    // Disable until FFDC timing issue is rectified (52921)
    //    @Test
    //    public void testUnknownProviderPersistenceXML() throws Exception {
    //        final String testMethod = "testUnknownProviderPersistenceXML";
    //        executeTest(UNMANAGED_APP, UNMANAGED_APP, testMethod);
    //    }

    /**
     * This test makes sure that an EM can persist an entity when not on a server
     * managed thread when it has not yet gotten the validation factory.
     *
     * Note: in order for this test to be *useful* it must be run before testJPAValidationServlet.
     * There's no way to ensure this, but we order the methods so that it has
     * a better chance of happening.
     */
    @Test
    public void testJPANonJ2eeThreadValidationServlet() throws Exception {
        String host = server.getMachine().getHostname();
        URL url = new URL("http://" + host + ":" + server.getHttpDefaultPort() + "/validation/JpaNonJ2eeThreadServlet");
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();
        svLogger.info(line);
        assertEquals("This is JpaNonJ2eeThreadServlet.", line);
        line = br.readLine();
        svLogger.info(line);
        assertEquals("SUCCESS: JPA Validation in non-J2EE thread works.", line);
        con.disconnect();
    }

    @Test
    public void testJPAValidationServlet() throws ProtocolException, IOException {
        String host = server.getMachine().getHostname();
        URL url = new URL("http://" + host + ":" + server.getHttpDefaultPort() + "/validation");
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();
        assertEquals("This is JpaValidationServlet.", line);
        line = br.readLine();
        assertEquals("SUCCESS: JPA Validation works well", line);
        con.disconnect();
    }

    private void executeTest(String app, String servlet, String testMethod) throws Exception {
        String retVal = callTestServlet(server, app, servlet, testMethod);

        if (retVal != null) {
            assertTrue("Servlet output did not contain the expected message (SUCCESS:" + testMethod + ")",
                       retVal.contains("SUCCESS:" + testMethod));
        } else {
            fail("ERROR: No result returned from servlet.  Check server log for failure.");
        }
        svLogger.finest("JPAFATTest." + testMethod + " : " + retVal);
    }

    private void validateServletOutput(BufferedReader br, String expectedEntityManager) throws IOException {
        /*
         * Check the first and last lines and make sure
         * there are no errors in between.
         */
        String line = null;
        line = br.readLine();
        String lastLine = line;
        assertEquals(THIS_IS_JPA_SERVLET, line);
        while (line != null) {
            lastLine = line;
            if (line.startsWith("Created EntityManager of type: ")) {
                assertEquals("Wrong entity manager.", "Created EntityManager of type: " + expectedEntityManager, line);
            }
            assertFalse("A problem was reported: " + line, line.contains("ERROR"));
            line = br.readLine();

        }
        assertEquals(A_PERSISTENCE_PROVIDER_IS_PRESENT_AND_WORKING, lastLine);
    }

    /**
     * @param br
     * @param expectedEntityManagerType
     * @throws IOException
     */
    private void validateCreateServletOutput(BufferedReader br, Set<Integer> ids, String expectedEntityManagerType) throws IOException {
        /*
         * Check the first and last lines and make sure
         * there are no errors in between.
         */
        String line = null;
        line = br.readLine();
        boolean thingFound = false;
        assertEquals(THIS_IS_JPA_SERVLET, line);
        StringBuffer output = new StringBuffer();
        while (line != null) {
            output.append(line);
            output.append("\n");
            if (line.startsWith("Created EntityManager of type: ")) {
                assertEquals("Wrong entity manager.", "Created EntityManager of type: " + expectedEntityManagerType, line);
            } else if (line.startsWith("Created Thing:")) {
                {
                    assertFalse("Thing creation went wrong: " + line, line.contains("null"));
                    // Occasionally Derby is not sequential in generating IDs, so as long as
                    // the ID is unique things are fine. Since Derby just skips a sequential
                    // number sometimes, just checking for > should be sufficient.
                    int actualId = extractId(line);
                    boolean added = ids.add(actualId);
                    assertTrue("Expected unique id: " + line + ", previous: " + ids, added);
                }
                thingFound = true;

            }
            assertFalse("A problem was reported: " + line, line.contains("ERROR"));
            line = br.readLine();

        }
        assertTrue("No thing was created. The output was \n" + output, thingFound);
    }

    private int extractId(String line) {
        try {
            int beginIndex = line.indexOf("[") + 1;
            int endIndex = line.indexOf("]");

            if (beginIndex > 0 && endIndex > beginIndex) {
                String idStr = line.substring(beginIndex, endIndex);
                int id = Integer.parseInt(idStr);
                svLogger.info("Extracted ID = " + id);
                return id;
            }
        } catch (Exception ex) {
            svLogger.info("Unable to extract ID from : " + line + " : exception = " + ex);
        }

        svLogger.info("Extracted ID = -1");
        return -1;
    }

    /**
     * @param br
     * @param expectedEntityManager
     * @throws IOException
     */
    private Set<Integer> validateRetrieveServletOutput(BufferedReader br, String expectedEntityManager) throws IOException {
        /*
         * Check the first and last lines and make sure
         * there are no errors in between.
         */
        String line = null;
        line = br.readLine();
        assertEquals(THIS_IS_JPA_SERVLET, line);
        Set<Integer> ids = new LinkedHashSet<Integer>();
        int count = -1;
        while (line != null) {
            Log.info(JPAFATTest.class, "validateRetrieveServletOutput", "LINE: " + line);
            if (line.startsWith("Created EntityManager of type: ")) {
                assertEquals("Wrong entity manager.", "Created EntityManager of type: " + expectedEntityManager, line);
            } else if (line.startsWith("Thing in list ") || line.startsWith("LooseThing in list ")) {
                int actualId = extractId(line);
                boolean added = ids.add(actualId);
                assertTrue("Expected unique id: " + line + ", previous: " + ids, added);
            } else {
                String countPrefix = "Query returned ";
                if (line.startsWith(countPrefix)) {
                    count = Integer.parseInt(line.substring(countPrefix.length()));
                }

            }
            assertFalse("A problem was reported: " + line, line.contains("ERROR"));
            line = br.readLine();

        }
        Log.info(JPAFATTest.class, "validateRetrieveServletOutput", "count=" + count + ", ids.size()=" + ids.size());
        assertEquals(ids + ".size()", count, ids.size());
        return ids;
    }

    @Test
    public void testTraceChannel() throws Exception {
        RemoteFile remoteFile = server.getServerBootstrapPropertiesFile();
        Properties orig = new Properties();
        InputStream is = remoteFile.openForReading();
        try {
            orig.load(is);
        } finally {
            is.close();
        }
        try {
            // Update bootstrap.properties file with new trace spec
            Properties testProps = new Properties();
            testProps.putAll(orig);
            testProps.put("com.ibm.ws.logging.trace.specification", "openjpa.jdbc.SQL=ALL");
            OutputStream os = remoteFile.openForWriting(false);
            try {
                testProps.store(os, "");
            } finally {
                os.close();
            }
            //restart server
            server.stopServer(ALLOWED_MESSAGES);
            server.startServer();

            // Exercise
            testInjectedContainerManagedJPAServlet();

            server.waitForStringInTrace("openjpa.jdbc.SQL. Trace:");
        } finally {
            OutputStream os = remoteFile.openForWriting(false);
            try {
                orig.store(os, "");
            } finally {
                os.close();
            }
            server.stopServer(ALLOWED_MESSAGES);
            server.startServer();
        }
    }
}
