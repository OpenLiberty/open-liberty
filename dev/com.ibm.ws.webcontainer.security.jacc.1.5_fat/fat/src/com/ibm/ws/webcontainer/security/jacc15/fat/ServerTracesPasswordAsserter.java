/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security.jacc15.fat;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import componenttest.topology.impl.LibertyServer;

/**
 * Helper class to assert that there are no password in the server's log and trace files.
 */
public class ServerTracesPasswordAsserter {

    private final LibertyServer server;
    private List<String> passwordsInTrace;
    private final Map<String, List<String>> previousPasswordsInTracePerPassword;

    public ServerTracesPasswordAsserter(LibertyServer server) {
        this.server = server;
        this.passwordsInTrace = Collections.emptyList();
        this.previousPasswordsInTracePerPassword = new HashMap<String, List<String>>();
    }

    public void assertNoPasswords(String... passwords) throws Exception {
        for (String password : passwords) {
            assertNoPassword(password);
        }
    }

    public void assertNoNewPasswords(String... passwords) throws Exception {
        for (String password : passwords) {
            assertNoNewPassword(password);
        }
    }

    public void assertNoPassword(String password) throws Exception {
        passwordsInTrace = server.findStringsInLogsAndTrace(password);
        assertEquals("Should not find password in the log file",
                     Collections.emptyList(), passwordsInTrace);
    }

    /**
     * Tests that there are no new outputs in the server traces since the last call to
     * this method that contain the specified password, regardless that a previous call
     * to this method found such password in the traces. A test using this assertion
     * method will not fail if a previous test using this method failed, contrary to the
     * assertNoPassword usage where it takes in consideration all matching outputs
     * containing the password even when those were created by a previous call.
     *
     * @param password
     * @throws Exception
     */
    public void assertNoNewPassword(String password) throws Exception {
        List<String> previousPasswords = getPreviousPasswords(password);
        passwordsInTrace = server.findStringsInLogsAndTrace(password);
        if (passwordsInTrace.isEmpty() == false) {
            setPreviousPasswords(password, passwordsInTrace);
            assertEquals("Should not find a new password in the log file for password " + password,
                         previousPasswords, passwordsInTrace);
        }
    }

    private List<String> getPreviousPasswords(String password) {
        List<String> previousPasswords = previousPasswordsInTracePerPassword.get(password);
        if (previousPasswords == null) {
            previousPasswords = Collections.emptyList();
        }
        return previousPasswords;
    }

    private void setPreviousPasswords(String password, List<String> previousPasswords) {
        previousPasswordsInTracePerPassword.put(password, previousPasswords);
    }

}
