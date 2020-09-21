/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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

import com.ibm.testapp.g3store.restConsumer.client.ConsumerEndpointFATServlet;
import com.ibm.testapp.g3store.restProducer.client.ProducerEndpointFATServlet;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * @author anupag
 *
 */
@RunWith(FATRunner.class)
public class StoreServicesRESTClientTests extends FATServletClient {

    protected static final Class<?> c = StoreServicesRESTClientTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("StoreServer")
    public static LibertyServer storeServer;

    @Server("ProducerServer")
    @TestServlet(servlet = ProducerEndpointFATServlet.class, contextRoot = "StoreProducerApp")
    public static LibertyServer producerServer;

    @Server("ConsumerServer")
    @TestServlet(servlet = ConsumerEndpointFATServlet.class, contextRoot = "StoreConsumerApp")
    public static LibertyServer consumerServer;

    private static String getSysProp(String key) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key));
    }

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive store_war = ShrinkHelper.defaultApp(storeServer, "StoreApp.war",
                                                       "com.ibm.testapp.g3store.cache",
                                                       "com.ibm.testapp.g3store.exception",
                                                       "com.ibm.testapp.g3store.interceptor",
                                                       "com.ibm.testapp.g3store.grpcservice",
                                                       "com.ibm.testapp.g3store.servletStore",
                                                       "com.ibm.testapp.g3store.utilsStore",
                                                       "com.ibm.test.g3store.grpc"); // add generated src

        WebArchive producer_war = ShrinkHelper.defaultDropinApp(producerServer, "StoreProducerApp.war",
                                                                "com.ibm.testapp.g3store.grpcProducer.api",
                                                                "com.ibm.testapp.g3store.exception",
                                                                "com.ibm.testapp.g3store.restProducer",
                                                                "com.ibm.testapp.g3store.restProducer.api",
                                                                "com.ibm.testapp.g3store.restProducer.model",
                                                                "com.ibm.testapp.g3store.restProducer.client",
                                                                "com.ibm.testapp.g3store.servletProducer",
                                                                "com.ibm.testapp.g3store.utilsProducer",
                                                                "com.ibm.ws.fat.grpc.monitoring",
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
                                                          "com.ibm.testapp.g3store.utilsProducer",
                                                          "com.ibm.test.g3store.grpc", // add generated src
                                                          "com.ibm.testapp.g3store.restProducer.client");

        storeServer.startServer(StoreServicesRESTClientTests.class.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not recieved", storeServer.waitForStringInLog("CWWKO0219I.*ssl"));

        producerServer.useSecondaryHTTPPort(); // sets httpSecondaryPort and httpSecondarySecurePort
        producerServer.startServer(StoreServicesRESTClientTests.class.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not recieved", producerServer.waitForStringInLog("CWWKO0219I.*ssl"));

        // set bvt.prop.member_1.http=8080 and bvt.prop.member_1.https=8081
        consumerServer.setHttpDefaultPort(Integer.parseInt(getSysProp("member_1.http")));
        int securePort = Integer.parseInt(getSysProp("member_1.https"));

        Log.info(StoreServicesRESTClientTests.class, "setUp", "here is the secure port " + securePort);

        consumerServer.setHttpDefaultSecurePort(securePort);
        consumerServer.startServer(StoreServicesRESTClientTests.class.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not recieved", consumerServer.waitForStringInLog("CWWKO0219I.*ssl"));

        // To export the assembled services application archive files, uncomment the following
        // run it locally , keep them commented when merging

//        ShrinkHelper.exportArtifact(store_war, "publish/savedApps/StoreServer/");
//        ShrinkHelper.exportArtifact(producer_war, "publish/savedApps/ProducerServer/");
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
            //Expected failures

            //CWIML4537E: The login operation could not be completed.
            //The specified principal name dev2 is not found in the back-end repository.

            //CWWKS1725E: The resource server failed to validate the access token
            //because the validationEndpointUrl [null] was either not a valid URL
            // or could not perform the validation.

            //CWWKS1737E: The OpenID Connect client [null] failed to validate the JSON Web Token.
            //The cause of the error was: [CWWKS1781E: Validation failed for the token requested
            //by the client [null] because the (iss) issuer [testIssuer] that is specified in
            //the token does not match any of the trusted issuers [testIssuerBad]
            //that are specified by the [issuerIdentifier] attribute of the OpenID
            //Connect client configuration.]

            if (storeServer != null)
                storeServer.stopServer("SRVE9967W", "CWIML4537E", "CWWKS1725E", "CWWKS1737E", "CWWKT0204E", "CWWKT0205E");
        } catch (Exception e) {
            excep = e;
            Log.error(c, "store tearDown", e);
        }

        try {
            if (consumerServer != null)
                consumerServer.stopServer("SRVE9967W");
        } catch (Exception e) {
            if (excep == null)
                excep = e;
            Log.error(c, "consumer tearDown", e);
        }

        try {
            if (producerServer != null)
                producerServer.stopServer();
        } catch (Exception e) {
            if (excep == null)
                excep = e;
            Log.error(c, "producer tearDown", e);
        }

        if (excep != null)
            throw excep;
    }

    @Test
    public void testStoreWarStartWithGrpcService() throws Exception {
        Log.info(getClass(), "testStoreWarStartWithGrpcService", "Check if Store.war started");
        assertNotNull(storeServer.waitForStringInLog("CWWKZ0001I: Application StoreApp started"));

    }

    @Test
    public void testProducerWarStartWithGrpcService() throws Exception {
        Log.info(getClass(), "testProducerWarStartWithGrpcService", "Check if Prodcuer.war started");
        assertNotNull(producerServer.waitForStringInLog("CWWKZ0001I: Application StoreProducerApp started"));

    }

    @Test
    public void testConsumerWarStartWithGrpcService() throws Exception {
        Log.info(getClass(), "testConsumerWarStartWithGrpcService", "Check if Consumer.war started");
        assertNotNull(consumerServer.waitForStringInLog("CWWKZ0001I: Application StoreConsumerApp started"));

    }

}
