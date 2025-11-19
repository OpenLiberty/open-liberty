/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.utils;

import java.util.HashSet;
import java.util.Set;

import org.junit.rules.ExternalResource;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpRequest;

/**
 *
 */
public class ToolStatusClient extends ExternalResource implements ToolStatus {

    private LibertyServer server;
    private String urlPrefix;
    private Set<String> latchesUsed = new HashSet<>();

    /**
     * @param server the server to call
     * @param urlPrefix the context root for the app containing ToolStatusServlet
     */
    public ToolStatusClient(LibertyServer server, String urlPrefix) {
        super();
        this.server = server;
        this.urlPrefix = urlPrefix;
    }

    @Override
    protected void after() {
        for (String latch : new HashSet<>(latchesUsed)) {
            signalShouldEnd(latch);
        }
    }

    @Override
    public void signalStarted(String latchName) {
        callServer("signalStarted", latchName);
    }

    @Override
    public void awaitStarted(String latchName) {
        callServer("awaitStarted", latchName);
    }

    @Override
    public void signalShouldEnd(String latchName) {
        callServer("signalShouldEnd", latchName);
    }

    @Override
    public void awaitShouldEnd(String latchName) {
        callServer("awaitShouldEnd", latchName);
    }

    private void callServer(String method, String latchName) {
        latchesUsed.add(latchName);
        try {
            new HttpRequest(server, urlPrefix, "/toolStatus", "/", method, "/", latchName).method("POST").run(String.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
