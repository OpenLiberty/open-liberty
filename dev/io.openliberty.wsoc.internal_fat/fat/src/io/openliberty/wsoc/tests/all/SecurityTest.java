/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.tests.all;

import java.io.IOException;

import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.endpoints.client.secure.SecureClientEP;

/**
 * Tests WebSocket Stuff
 * 
 * @author unknown
 */
public class SecurityTest {

    private WsocTest wsocTest = null;

    public SecurityTest(WsocTest test) {
        this.wsocTest = test;
    }

    /*
     * ServerEndpoint - @see SecureTextServerEP
     */
    public void testAnnotatedSecureSuccess() throws Exception {

        String[] textValues = { "1008" };
        wsocTest.runEchoTest(new SecureClientEP("user1", "security"), "/secure/endpoints/annotatedSecureText", textValues);

    }

    /*
     * ServerEndpoint - @see SecureTextServerEP
     */
    public void testAnnotatedSecureForbidden() throws Exception {

        String[] textValues = { "1008" };

        try {
            wsocTest.runEchoTest(new SecureClientEP("user2", "security"), "/secure/endpoints/annotatedSecureText", textValues);
            throw new Exception("Expected IOException with a 403 response, but did not receive one.");
        } catch (IOException e) {
            if (e.getMessage().indexOf("403") == -1) {
                throw new IOException("IOException returned, but no 403 response code", e);
            }
        }

    }

    /*
     * ServerEndpoint - @see SecureTextServerEP
     */
    public void testWSSRequired() throws Exception {

        String[] textValues = { "1000" };
        wsocTest.setSecure(false);

        //  Use non secure port for this test...
        int savedPort = wsocTest.getPort();
        wsocTest.setPort(wsocTest.getAltPort());
        try {
            wsocTest.runEchoTest(new SecureClientEP("user1", "security"), "/secure/endpoints/annotatedSecureText", textValues);
            throw new Exception("Expected IOException with a 302 response, but did not receive one.");
        } catch (IOException e) {
            if (e.getMessage().indexOf("302") == -1) {
                throw new IOException("IOException returned, but no 302 response code", e);
            }
        } finally {
            wsocTest.setSecure(true);
            wsocTest.setPort(savedPort);
        }

    }
}