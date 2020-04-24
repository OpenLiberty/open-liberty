/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.context.serialization;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.codec.binary.Base64;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PortType;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class ContextServiceSerializationTest extends FATServletClient {
    @Server("com.ibm.ws.context.fat.serialization")
    //@TestServlet(servlet = ContextServiceSerializationTestServlet.class, path = "contextserbvt/ContextServiceSerializationTestServlet")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "contextserbvt.war")
                        .addPackage("test.context.serialization.web")
                        .addAsWebInfResource(new File("test-applications/contextserbvt.war/resources/WEB-INF/web.xml"))
                        .addAsWebInfResource(new File("test-applications/contextserbvt.war/resources/WEB-INF/serialized/classloaderContext-EJB-v8.5.5.4.ser"),
                                             "serialized/classloaderContext-EJB-v8.5.5.4.ser")
                        .addAsWebInfResource(new File("test-applications/contextserbvt.war/resources/WEB-INF/serialized/classloaderContext-v8.5.5.4.ser"),
                                             "serialized/classloaderContext-v8.5.5.4.ser")
                        .addAsWebInfResource(new File("test-applications/contextserbvt.war/resources/WEB-INF/serialized/defaultContext_ALL_CONTEXT_TYPES-v8.5.5.4.ser"),
                                             "serialized/defaultContext_ALL_CONTEXT_TYPES-v8.5.5.4.ser")
                        .addAsWebInfResource(new File("test-applications/contextserbvt.war/resources/WEB-INF/serialized/jeeMetadataContext-EJB-v8.5.5.4.ser"),
                                             "serialized/jeeMetadataContext-EJB-v8.5.5.4.ser")
                        .addAsWebInfResource(new File("test-applications/contextserbvt.war/resources/WEB-INF/serialized/jeeMetadataContext-JSP-v8.5.5.4.ser"),
                                             "serialized/jeeMetadataContext-JSP-v8.5.5.4.ser")
                        .addAsWebInfResource(new File("test-applications/contextserbvt.war/resources/WEB-INF/serialized/jeeMetadataContext-v8.5.5.4.ser"),
                                             "serialized/jeeMetadataContext-v8.5.5.4.ser")
                        .addAsWebInfResource(new File("test-applications/contextserbvt.war/resources/WEB-INF/serialized/securityContext-user3-user3-v8.5.5.4.ser"),
                                             "serialized/securityContext-user3-user3-v8.5.5.4.ser")
                        .addAsWebInfResource(new File("test-applications/contextserbvt.war/resources/WEB-INF/serialized/transactionContext-v8.5.5.4.ser"),
                                             "serialized/transactionContext-v8.5.5.4.ser")
                        .addAsWebResource(new File("test-applications/contextserbvt.war/resources/SerializationTestJSP.jsp"));

        JavaArchive ejb = ShrinkWrap.create(JavaArchive.class, "contextserejb.jar")
                        .addPackage("test.context.serialization.app")
                        .addAsManifestResource(new File("test-applications/contextserejb.jar/resources/META-INF/ejb-jar.xml"));

        EnterpriseArchive app = ShrinkWrap.create(EnterpriseArchive.class, "contextserbvt.ear");
        app.addAsModules(war, ejb);

        ShrinkHelper.exportAppToServer(server, app);

        server.addInstalledAppForValidation("contextserbvt");

        server.startServer();
        assertNotNull(server.waitForStringInLog("CWWKS0008I")); // also wait for security service to start
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(
                          "CWNEN0046W.*javax", // TODO temporarily ignore until transformer supports deployment descriptors
                          "CWNEN0047W.*javax" // TODO temporarily ignore until transformer supports deployment descriptors
        );
    }

    /**
     * Utility method to run a test on the JSP or servlet.
     *
     * @param test     Test name to supply as an argument to the JSP or servlet
     * @param user     user name
     * @param password password
     * @return output of the JSP or servlet
     * @throws Exception if an error occurs
     */
    private StringBuilder runIn(String urlPattern, String test, final String user, final String password) throws Exception {

        URL url = new URL("http://localhost:" + server.getPort(PortType.WC_defaulthost) + "/contextserbvt/" + urlPattern + "?test=" + test);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        // Set the Authorization header if applicable.
        if (user != null) {
            String encodedAuthData = new String(Base64.encodeBase64(new String(user + ":" + password).getBytes("UTF-8")));
            con.setRequestProperty("Authorization", "Basic " + encodedAuthData);
        }

        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine())
                lines.append(line).append(sep);

            if (lines.indexOf("COMPLETED SUCCESSFULLY") < 0)
                fail("Missing success message in output. " + lines);

            return lines;
        } finally {
            con.disconnect();
        }
    }

    @Test
    public void testDeserializeClassloaderContext() throws Exception {
        runIn("TestServlet", "testDeserializeClassloaderContextV8_5_5_4", "user1", "pwd1");
    }

    @Test
    public void testDeserializeClassloaderContextThatCameFromEJB() throws Exception {
        runIn("TestServlet", "testDeserializeClassloaderContextV8_5_5_4_EJB", "user1", "pwd1");
    }

    @Test
    public void testDeserializeDefaultClassloaderContext() throws Exception {
        runIn("TestServlet", "testDeserializeDefaultClassloaderContextV8_5_5_4", "user1", "pwd1");
    }

    @Test
    public void testDeserializeDefaultJEEMetadataContext() throws Exception {
        runIn("TestServlet", "testDeserializeDefaultJEEMetadataContextV8_5_5_4", "user1", "pwd1");
    }

    @Test
    public void testDeserializeDefaultSecurityContext() throws Exception {
        runIn("TestServlet", "testDeserializeDefaultSecurityContextV8_5_5_4", "user2", "pwd2");
    }

    @Test
    public void testDeserializeDefaultTransactionContext() throws Exception {
        runIn("TestServlet", "testDeserializeDefaultTransactionContextV8_5_5_4", "user1", "pwd1");
    }

    @Test
    public void testDeserializeExecutionProperties() throws Exception {
        runIn("TestServlet", "testDeserializeExecutionPropertiesV8_5_5_4", "user1", "pwd1");
    }

    @Test
    public void testDeserializeJEEMetadataContext() throws Exception {
        runIn("TestServlet", "testDeserializeJEEMetadataContextV8_5_5_4", "user1", "pwd1");
    }

    @Test
    public void testDeserializeJEEMetadataContextThatCameFromJSP() throws Exception {
        runIn("TestServlet", "testDeserializeJEEMetadataContextV8_5_5_4_JSP", "user1", "pwd1");
    }

    @Test
    public void testDeserializeJEEMetadataContextThatCameFromEJB() throws Exception {
        runIn("TestServlet", "testDeserializeJEEMetadataContextV8_5_5_4_EJB", "user1", "pwd1");
    }

    @Test
    public void testDeserializeSecurityContext() throws Exception {
        runIn("TestServlet", "testDeserializeSecurityContextV8_5_5_4", "user2", "pwd2");
    }

    @Test
    public void testDeserializeTransactionContext() throws Exception {
        runIn("TestServlet", "testDeserializeTransactionContextV8_5_5_4", "user1", "pwd1");
    }

    @Test
    public void testProxyForAProxy() throws Exception {
        runIn("TestServlet", "testProxyForAProxy", "user1", "pwd1");
    }

    @Test
    public void testReserializeJEEMetadataContext() throws Exception {
        runIn("TestServlet", "testReserializeJEEMetadataContext", "user1", "pwd1");
    }

    @Test
    public void testSerializeClassloaderContext() throws Exception {
        runIn("TestServlet", "testSerializeClassloaderContext", "user1", "pwd1");
    }

    @Test
    public void testSerializeClassloaderContextFromEJB() throws Exception {
        runIn("TestServlet", "testSerializeClassloaderContext_EJB", "user1", "pwd1");
    }

    @Test
    public void testSerializeDefaultContext_ALL_CONTEXT_TYPES() throws Exception {
        runIn("TestServlet", "testSerializeDefaultContext_ALL_CONTEXT_TYPES", "user1", "pwd1");
    }

    @Test
    public void testSerializeJEEMetadataContextFromJSP() throws Exception {
        runIn("TestJSP", "testSerializeJEEMetadataContext", "user1", "pwd1");
    }

    @Test
    public void testSerializeJEEMetadataContextFromServlet() throws Exception {
        runIn("TestServlet", "testSerializeJEEMetadataContext", "user1", "pwd1");
    }

    @Test
    public void testSerializeJEEMetadataContextFromEJB() throws Exception {
        runIn("TestServlet", "testSerializeJEEMetadataContext_EJB", "user1", "pwd1");
    }

    @Test
    public void testSerializeSecurityContext() throws Exception {
        runIn("TestServlet", "testSerializeSecurityContext", "user3", "pwd3");
    }

    @Test
    public void testSerializeTransactionContext() throws Exception {
        runIn("TestServlet", "testSerializeTransactionContext", "user1", "pwd1");
    }

}
