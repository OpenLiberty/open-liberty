/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.servers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.exceptions.FatExceptionUtils;

import componenttest.topology.impl.LibertyServer;

public class ServerTracker {

    protected static Class<?> thisClass = ServerTracker.class;

    Set<LibertyServer> libertyServers = new HashSet<LibertyServer>();

    public void addServer(LibertyServer server) {
        libertyServers.add(server);
        if (server == null) {
            return;
        }
        // Every server should have the testMarker app in dropins (make sure it starts)
        server.addInstalledAppForValidation(Constants.APP_TESTMARKER);
    }

    public Set<LibertyServer> getServers() {
        return new HashSet<LibertyServer>(libertyServers);
    }

    public void stopAllServers() throws Exception {
        List<Exception> exceptions = new ArrayList<Exception>();
        for (LibertyServer server : libertyServers) {
            try {
                stopServerOrThrowException(server);
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            throw FatExceptionUtils.buildCumulativeException(exceptions);
        }
    }

    /**
     * The normal stopAllServers will go down a path that will
     * try to back up servers that were stopped and already backed up
     * In some special cases, that does not work out so well...
     * This method will only call stop if the server is still running.
     * 
     * @throws Exception
     */
    public void stopAllRunningServers() throws Exception {
        List<Exception> exceptions = new ArrayList<Exception>();
        for (LibertyServer server : libertyServers) {
            try {
                if (server.isStarted()) {
                    stopServerOrThrowException(server);
                }
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            throw FatExceptionUtils.buildCumulativeException(exceptions);
        }
    }

    void stopServerOrThrowException(LibertyServer server) throws Exception {
        if (server == null) {
            return;
        }
        try {
            server.stopServer();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw e;
        }
    }

}
