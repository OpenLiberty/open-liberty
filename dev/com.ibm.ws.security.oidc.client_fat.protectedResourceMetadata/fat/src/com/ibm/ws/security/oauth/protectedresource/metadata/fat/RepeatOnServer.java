/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.oauth.protectedresource.metadata.fat;

import componenttest.rules.repeater.RepeatTestAction;

/**
 * Repeat tests on multiple servers
 * <p>
 * Tests must call {@link #getServerName()} to find the server name for the current repeat
 */
public class RepeatOnServer implements RepeatTestAction {

    private static String CURRENT_SERVER;

    public static String getServerName() {
        return CURRENT_SERVER;
    }

    private final String id;
    private final String serverName;
    private String priorServer;

    public RepeatOnServer(String id, String serverName) {
        this.id = id;
        this.serverName = serverName;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setup() throws Exception {
        priorServer = CURRENT_SERVER;
        CURRENT_SERVER = serverName;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public void cleanup() throws Exception {
        CURRENT_SERVER = priorServer;
        priorServer = null;
    }

}
