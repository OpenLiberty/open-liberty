/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.fat.grpc;

import static org.junit.Assert.assertNotNull;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.testapp.g3store.restConsumer.client.ConsumerEndpointFATServlet;
import com.ibm.testapp.g3store.restProducer.client.ProducerEndpointFATServlet;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * @author anupag
 *
 */
@RunWith(FATRunner.class)
// Skip for EE11: OpenAPI NPE due to @OAuthScope having a name but no description in ConsumerRestAppWrapper.
@SkipForRepeat(SkipForRepeat.EE11_FEATURES)
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

        storeServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        producerServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        consumerServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));

        boolean isArchive = false;
        // To export the assembled services application archive files, set isArchive to true
        // run it locally , keep this false when merging

        StoreClientTestsUtils.addStoreApp(storeServer, isArchive);

        StoreClientTestsUtils.addProducerApp(producerServer, isArchive);

        StoreClientTestsUtils.addConsumerApp_RestClient(consumerServer, isArchive);

        storeServer.startServer(c.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not received", storeServer.waitForStringInLog("CWWKO0219I.*ssl"));

        producerServer.useSecondaryHTTPPort(); // sets httpSecondaryPort and httpSecondarySecurePort
        producerServer.startServer(c.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not received", producerServer.waitForStringInLog("CWWKO0219I.*ssl"));

        // set bvt.prop.member_1.http=8080 and bvt.prop.member_1.https=8081
        consumerServer.setHttpDefaultPort(Integer.parseInt(getSysProp("member_1.http")));
        int securePort = Integer.parseInt(getSysProp("member_1.https"));

        Log.info(StoreServicesRESTClientTests.class, "setUp", "here is the secure port " + securePort);

        consumerServer.setHttpDefaultSecurePort(securePort);
        consumerServer.startServer(c.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not received", consumerServer.waitForStringInLog("CWWKO0219I.*ssl"));

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
    public void testProducerWarStartWithGrpcClient() throws Exception {
        Log.info(getClass(), "testProducerWarStartWithGrpcClient", "Check if Prodcuer.war started");
        assertNotNull(producerServer.waitForStringInLog("CWWKZ0001I: Application StoreProducerApp started"));

    }

    @Test
    public void testConsumerWarStartWithGrpcClient() throws Exception {
        Log.info(getClass(), "testConsumerWarStartWithGrpcClient", "Check if Consumer.war started");
        assertNotNull(consumerServer.waitForStringInLog("CWWKZ0001I: Application StoreConsumerApp started"));

    }

}
