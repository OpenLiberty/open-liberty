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

import com.ibm.testapp.g3store.restConsumer.client.ConsumerEndpointJWTCookieFATServlet;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 */
@RunWith(FATRunner.class)
// Skip for EE11: OpenAPI NPE due to @OAuthScope having a name but no description in ConsumerRestAppWrapper.
@SkipForRepeat(SkipForRepeat.EE11_FEATURES)
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

        storeJWTSSoServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        consumerServer.addIgnoredErrors(Arrays.asList("CWPKI0063W"));

        boolean isArchive = false;
        // To export the assembled services application archive files, set isArchive to true
        // run it locally , keep this false when merging
        StoreClientTestsUtils.addStoreApp(storeJWTSSoServer, isArchive);

        StoreClientTestsUtils.addConsumerApp_RestClient(consumerServer, isArchive);

        storeJWTSSoServer.startServer(c.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not received", storeJWTSSoServer.waitForStringInLog("CWWKO0219I.*ssl"));

        // set bvt.prop.member_1.http=8080 and bvt.prop.member_1.https=8081
        consumerServer.setHttpDefaultPort(Integer.parseInt(getSysProp("member_1.http")));
        int securePort = Integer.parseInt(getSysProp("member_1.https"));

        Log.info(c, "setUp", "here is the secure port " + securePort);

        consumerServer.setHttpDefaultSecurePort(securePort);
        consumerServer.startServer(c.getSimpleName() + ".log");
        assertNotNull("CWWKO0219I.*ssl not received", consumerServer.waitForStringInLog("CWWKO0219I.*ssl"));

    }

    //Similar to these are added in logs and we can ignore
    //SRVE9967W: The manifest class path xml-apis.jar can not be found in jar file wsjar:file:/.../open-liberty/dev/build.image/wlp/usr/servers/StoreServer/apps/StoreApp.war!/WEB-INF/lib/serializer-2.7.2.jar or its parent.
    //SRVE9967W: The manifest class path xercesImpl.jar can not be found in jar file wsjar:file:/.../open-liberty/dev/build.image/wlp/usr/servers/StoreServer/apps/StoreApp.war!/WEB-INF/lib/xalan-2.7.2.jar or its parent.
    @AfterClass
    public static void tearDown() throws Exception {
        Exception excep = null;

        try {

            //CWWKT0206E: gRPC service request for test.g3store.grpc.AppConsumerService/getAppSetBadRoleCookieJWTHeader
            // failed with authorization error Unauthorized.
            if (storeJWTSSoServer != null)
                storeJWTSSoServer.stopServer("SRVE9967W", "CWWKT0206E");
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
