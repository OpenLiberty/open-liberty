/**
 *
 */
package com.ibm.ws.fat.grpc;

import static org.junit.Assert.assertNotNull;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.testapp.g3store.restConsumer.client.ConsumerEndpointJWTCookieFATServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */

@RunWith(FATRunner.class)
public class StoreServicesSecurityTests extends FATServletClient {

    protected static final Class<?> c = StoreServicesSecurityTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("StoreJWTSSoServer")
    public static LibertyServer storeJWTSSoServer;

    @Server("ConsumerServer")
    @TestServlet(servlet = ConsumerEndpointJWTCookieFATServlet.class, contextRoot = "StoreConsumerApp")
    public static LibertyServer consumerServer;

    private static String getSysProp(String key) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key));
    }

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive store_war = ShrinkHelper.defaultApp(storeJWTSSoServer, "StoreApp.war",
                                                       "com.ibm.testapp.g3store.cache",
                                                       "com.ibm.testapp.g3store.exception",
                                                       "com.ibm.testapp.g3store.interceptor",
                                                       "com.ibm.testapp.g3store.grpcservice",
                                                       "com.ibm.testapp.g3store.servletStore",
                                                       "com.ibm.testapp.g3store.utilsStore",
                                                       "com.ibm.test.g3store.grpc"); // add generated src

        // Use defaultApp the <application> element is used in server.xml for security, cannot use dropin
        // The consumer tests needs to create data also , we will need to add producer files also
        WebArchive consumer_war = ShrinkHelper.defaultApp(consumerServer, "StoreConsumerApp.war",
                                                          "com.ibm.testapp.g3store.grpcConsumer.api",
                                                          "com.ibm.testapp.g3store.grpcConsumer.security",
                                                          "com.ibm.testapp.g3store.exception",
                                                          "com.ibm.testapp.g3store.restConsumer",
                                                          "com.ibm.testapp.g3store.restConsumer.api",
                                                          "com.ibm.testapp.g3store.restConsumer.model",
                                                          "com.ibm.testapp.g3store.servletConsumer",
                                                          "com.ibm.testapp.g3store.utilsConsumer",
                                                          "com.ibm.testapp.g3store.restConsumer.client",
                                                          "com.ibm.testapp.g3store.grpcProducer.api",
                                                          "com.ibm.testapp.g3store.restProducer",
                                                          "com.ibm.testapp.g3store.restProducer.api",
                                                          "com.ibm.testapp.g3store.restProducer.model",
                                                          "com.ibm.testapp.g3store.servletProducer",
                                                          "com.ibm.test.g3store.grpc", // add generated src
                                                          "com.ibm.testapp.g3store.restProducer.client");

        storeJWTSSoServer.startServer(c.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not recieved", storeJWTSSoServer.waitForStringInLog("CWWKO0219I.*ssl"));

        // set bvt.prop.member_1.http=8080 and bvt.prop.member_1.https=8081
        consumerServer.setHttpDefaultPort(Integer.parseInt(getSysProp("member_1.http")));
        int securePort = Integer.parseInt(getSysProp("member_1.https"));

        Log.info(c, "setUp", "here is the secure port " + securePort);

        consumerServer.setHttpDefaultSecurePort(securePort);
        consumerServer.startServer(c.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not recieved", consumerServer.waitForStringInLog("CWWKO0219I.*ssl"));

        // To export the assembled services application archive files, uncomment the following
        // run it locally , keep them commented when merging

//        ShrinkHelper.exportArtifact(store_war, "publish/savedApps/StoreJWTSSoServer/");
//        ShrinkHelper.exportArtifact(consumer_war, "publish/savedApps/ConsumerServer/");
//

        //once this war file is installed on external Server
        // send the request e.g.
        // URL=http://localhost:8030/StoreProducerApp/ProducerEndpointFATServlet?testMethod=testClientStreaming

    }

    //Similar to these are added in logs and we can ignore
    //SRVE9967W: The manifest class path xml-apis.jar can not be found in jar file wsjar:file:/.../open-liberty/dev/build.image/wlp/usr/servers/StoreServer/apps/StoreApp.war!/WEB-INF/lib/serializer-2.7.2.jar or its parent.
    //SRVE9967W: The manifest class path xercesImpl.jar can not be found in jar file wsjar:file:/.../open-liberty/dev/build.image/wlp/usr/servers/StoreServer/apps/StoreApp.war!/WEB-INF/lib/xalan-2.7.2.jar or its parent.
    @AfterClass
    public static void tearDown() throws Exception {
        Exception excep = null;

        try {
            // CWWKT0202W need to fix this in grpc server code
            if (storeJWTSSoServer != null)
                storeJWTSSoServer.stopServer("SRVE9967W", "CWWKT0202W");
        } catch (Exception e) {
            excep = e;
            Log.error(c, "storeJWTSSoServer tearDown", e);
        }

        try {
            if (consumerServer != null)
                consumerServer.stopServer("SRVE9967W");
        } catch (Exception e) {
            if (excep == null)
                excep = e;
            Log.error(c, "consumer tearDown", e);
        }

        if (excep != null)
            throw excep;
    }

    @Test
    public void testStoreWarStartWithGrpcService() throws Exception {
        Log.info(getClass(), "testStoreWarStartWithGrpcService", "Check if Store.war started");
        assertNotNull(storeJWTSSoServer.waitForStringInLog("CWWKZ0001I: Application StoreApp started"));

    }

    @Test
    public void testConsumerWarStartWithGrpcService() throws Exception {
        Log.info(getClass(), "testConsumerWarStartWithGrpcService", "Check if Consumer.war started");
        assertNotNull(consumerServer.waitForStringInLog("CWWKZ0001I: Application StoreConsumerApp started"));

    }

}
