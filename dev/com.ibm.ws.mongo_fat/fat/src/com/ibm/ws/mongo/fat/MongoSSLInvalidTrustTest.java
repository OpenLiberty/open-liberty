/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mongo.fat;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class MongoSSLInvalidTrustTest extends FATServletClient {
    @Server("mongo.fat.server.ssl")
    public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        MongoServerSelector.assignMongoServers(server);
        FATSuite.createApp(server);
        server.startServer();
        FATSuite.waitForMongoSSL(server);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        // TODO: CWWKE0701E - Circular reference detected trying to get service
        // {org.osgi.service.cm.ManagedServiceFactory,
        // com.ibm.wsspi.logging.Introspector,
        // com.ibm.ws.runtime.update.RuntimeUpdateListener,
        // com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator}
        server.stopServer("CWPKI0823E:.*", // SSL HANDSHAKE FAILURE
                          "CWWKE0701E",
                          "CWWKG0033W");
    }

    @Before
    public void beforeEach() throws Exception {
        server.setMarkToEndOfLog();
    }

    @Test
    @AllowedFFDC({ "java.security.cert.CertPathBuilderException", "sun.security.validator.ValidatorException", "com.ibm.security.cert.IBMCertPathBuilderException" })
    public void testCertAuthInvalidTrust() throws Exception {
        testInvalidConfig("mongo/testdb-invalid-certificate-trust", "MongoTimeoutException");
        // CWPKI0823E: SSL HANDSHAKE FAILURE:  A signer with SubjectDN [{0}] was sent from the host [{1}].  The signer might
        // need to be added to local trust store [{2}], located in SSL configuration alias [{3}].  The extended error message from the SSL handshake exception is: [{4}].
        assertNotNull("Server exception for error CWPKI0823E was not found within the allotted interval", server.waitForStringInLogUsingMark("CWPKI0823E"));
        assertNotNull("SSLHandshakeException was not found within the allotted interval", server.waitForStringInLogUsingMark("SSLHandshakeException"));
    }

    private void testInvalidConfig(String jndiName, String expectedEx) throws Exception {
        FATServletClient.runTest(server, FATSuite.APP_NAME + "/MongoSSLTestServlet",
                                 "testInvalidConfig&jndiName=" + jndiName + "&expectedEx=" + expectedEx + "&forTest=" + testName.getMethodName());
    }
}
