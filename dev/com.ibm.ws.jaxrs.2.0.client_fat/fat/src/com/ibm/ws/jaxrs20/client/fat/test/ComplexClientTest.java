/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.fat.test;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class ComplexClientTest extends AbstractTest {

    @Server("jaxrs20.client.ComplexClientTest")
    public static LibertyServer server;

    private final static String appname = "complexclient";
    private final static String target = appname + "/ClientTestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname,
                                                       "com.ibm.ws.jaxrs20.client.ComplexClientTest.client",
                                                       "com.ibm.ws.jaxrs20.client.ComplexClientTest.service");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    @Test
    public void complexTestClientPropertyInherit() throws Exception {
        this.runTestOnServer(target, "testClientPropertyInherit", null, "{inherit1=cb},{inherit2=c, inherit1=cb},ECHO1:test");
    }

    @Test
    public void complexTestClientProviderInherit() throws Exception {
        this.runTestOnServer(target, "testClientProviderInherit", null, "banana,mango,3");
    }

    @Test
    public void complexTestNewClientBuilder() throws Exception {
        this.runTestOnServer(target, "testNewClientBuilder", null, "com.ibm.ws");
    }

    @Test
    public void complexTestNewClient() throws Exception {
        this.runTestOnServer(target, "testNewClient", null, "clientproperty2=somevalue2");
        this.runTestOnServer(target, "testNewClient", null, "clientproperty1=somevalue1");
    }

    @Test
    public void complexTestNewClientWithConfig() throws Exception {
        this.runTestOnServer(target, "testNewClientWithConfig", null, "clientproperty3=somevalue3");
        this.runTestOnServer(target, "testNewClientWithConfig", null, "clientproperty4=somevalue4");
    }

    @Test
    public void complexTestNewClientHostnameVerifier() throws Exception {
        this.runTestOnServer(target, "testNewClientHostnameVerifier", null, "client.ClientTestServlet");
    }

    @Test
    public void complexTestNewClientSslContext() throws Exception {
        this.runTestOnServer(target, "testNewClientSslContext", null, "javax.net.ssl.SSLContext");
    }

    @Test
    public void complexTestNew2WebTargets() throws Exception {
        this.runTestOnServer(target, "testNew2WebTargets", null, "ECHO1:test1,ECHO2:test2");
    }

    @Test
    public void complexTestNew2Invocations() throws Exception {
        this.runTestOnServer(target, "testNew2Invocations", null, "ECHO1:test3,ECHO2:test4");
    }

    @Test
    public void complexTestNew2FailedWebTargets() throws Exception {
        this.runTestOnServer(target, "testNew2FailedWebTargets", null, "ECHO1:test5,ECHO2:failed,ECHO3:failed");
    }

    @Test
    public void testWebTargetWithEncoding() throws Exception {
        this.runTestOnServer(target, "testWebTargetWithEncoding", null, "ECHO1:failed,ECHO1:test9");
    }

    @Test
    public void complexTestNew2WebTargetsRequestFilter() throws Exception {
        this.runTestOnServer(target, "testNew2WebTargetsRequestFilter", null, "{filter1=GET},{filter1=GET, filter2=*/*}");
    }

    @Test
    public void complexTestNew2ResponseFilter() throws Exception {
        this.runTestOnServer(target, "testNew2ResponseFilter", null, "222,223");
    }

    @Test
    public void complexTestNew2MixFilter() throws Exception {
        this.runTestOnServer(target, "testNew2MixFilter", null, "222,{filter1=GET},223,{filter2=null}");
    }

    @Test
    public void complexTestClientResponseProcessingException() throws Exception {
        this.runTestOnServer(target, "testClientResponseProcessingException", null, "Problem with reading the data");
    }

    @Test
    public void complexTestWebApplicationException3xx() throws Exception {
        this.runTestOnServer(target, "testWebApplicationException3xx", null, "HTTP 333");
    }

    @Test
    public void complexTestWebApplicationException4xx() throws Exception {
        this.runTestOnServer(target, "testWebApplicationException4xx", null, "HTTP 444");
    }

    @Test
    public void complexTestWebApplicationException5xx() throws Exception {
        this.runTestOnServer(target, "testWebApplicationException5xx", null, "HTTP 555");
    }

    @Test
    public void complexTestClientWithLtpaHander() throws Exception {
        this.runTestOnServer(target, "testClientLtpaHander_ClientNoToken", null, "ECHO1:test1");
    }

    @Test
    public void complexTestClientWithWrongLtpaHander() throws Exception {
        this.runTestOnServer(target, "testClientWrongLtpaHander_ClientNoToken", null, "ECHO1:test1");
    }

    @Test
    public void complexTestClientWithWrongValueLtpaHander() throws Exception {
        this.runTestOnServer(target, "testClientWrongValueLtpaHander_ClientNoToken", null, "ECHO1:test1");
    }

    @Test
    public void CTSTestClientToAccessTraceResource() throws Exception {
        this.runTestOnServer(target, "testTraceForCTS", null, "trace");
    }

    @Test
    public void CTSTestClientToAccessVariantResponse() throws Exception {
        this.runTestOnServer(target, "testVariantResponseForCTS", null, true + "");
    }

    @Test
    public void CTSTestClientThrowsException() throws Exception {
        this.runTestOnServer(target, "testThrowsExceptionForCTS", null, true + "");
    }

    @Test
    public void CTSTestClientMethodLinkUsedInInvocation() throws Exception {
        this.runTestOnServer(target, "testResourceMethodLinkUsedInInvocationForCTS", null, true + "," + true);
    }

    @Test
    public void testGetSetEntityStreamOnRequestFilter() throws Exception {
        this.runTestOnServer(target, "testGetSetEntityStreamOnRequestFilter", null, "ENTITY_STREAM_WORKS");
    }

    @Test
    public void testGetSetEntityStreamOnResponseFilter() throws Exception {
        this.runTestOnServer(target, "testGetSetEntityStreamOnResponseFilter", null, "ENTITY_STREAM_WORKS1");
    }

    @Test
    public void testTargetTemplateVariable() throws Exception {
        this.runTestOnServer(target, "testTargetTemplateVariable", null, "123");
    }
}