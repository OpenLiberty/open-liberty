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
package com.ibm.ws.ssl.keystore.fat;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;

@RunWith(FATRunner.class)
public class KeyStorePasswordTest extends FATServletClient {

    private static final Class<?> c = KeyStorePasswordTest.class;
    private static final String PASSWORD_TOO_SHORT = "CWPKI0808E";
    private static final String KEYSTORE_GENERATED = "CWPKI0803A";
    private static final String KEYSTORE_NOT_GENERATED = "CWPKI0819I";

    private LibertyServer server;

    @Before
    public void beforeEach() {
        server = LibertyServerFactory.getLibertyServer("ssl_fat_keystoreGen-" + testName.getMethodName());
    }

    @After
    public void afterEach() throws Exception {
        if (server != null && server.isStarted())
            server.stopServer();
    }

    @Test
    public void noPass() throws Exception {
        startServer();
        server.waitForStringInLog(KEYSTORE_NOT_GENERATED);
        server.stopServer();
    }

    @Test
    public void configuredPass() throws Exception {
        startServer();
        server.waitForStringInLog(KEYSTORE_GENERATED);
        server.stopServer();
        validateGeneratedKeyStore(server, "liberty");
    }

    @Test
    public void configuredInvalidPass() throws Exception {
        startServer();
        server.waitForStringInLog(PASSWORD_TOO_SHORT);
        server.stopServer(PASSWORD_TOO_SHORT);
    }

    @Test
    public void envPass() throws Exception {
        startServer();
        server.waitForStringInLog(KEYSTORE_GENERATED);
        server.stopServer();
        validateGeneratedKeyStore(server, server.getServerEnv().getProperty("keystore_password"));
    }

    @Test
    public void envInvalidPass() throws Exception {
        startServer();
        server.waitForStringInLog(PASSWORD_TOO_SHORT);
        server.stopServer(PASSWORD_TOO_SHORT);
    }

    @Test
    public void envAndConfiguredPass() throws Exception {
        startServer();
        server.waitForStringInLog(KEYSTORE_GENERATED);
        server.stopServer();
        validateGeneratedKeyStore(server, "liberty");
    }

    private void startServer() throws Exception {
        server.startServer(testName.getMethodName() + ".log");
    }

    private static void validateGeneratedKeyStore(LibertyServer server, String password) throws Exception {
        String m = "validateGeneratedKeyStore";
        File keystore = new File(server.getFileFromLibertyServerRoot("resources/security/key.p12").getAbsolutePath());
        if (!keystore.exists())
            fail("Keystore was not generated at location: " + keystore.getAbsolutePath());
        Log.info(c, m, "Keystore exists at " + keystore.getAbsolutePath());

        Log.info(c, m, "Verifying that keystore is accessible using password=" + password);
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new FileInputStream(keystore), password.toCharArray());
    }

}
