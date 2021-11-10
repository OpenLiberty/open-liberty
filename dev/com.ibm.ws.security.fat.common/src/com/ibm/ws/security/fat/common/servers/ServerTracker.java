/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.MessageConstants;
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
        addServerIgnoreErrorMessages(server);

    }

    /**
     * Add Warning/Error messages that should be ignored in all server logs
     * Additional errors can be added for specific server types by overriding/extending this method
     *
     * @param server
     *            - the server to add the message(s) to ignore to...
     */
    public void addServerIgnoreErrorMessages(LibertyServer server) {

        // ignore quiesce issues during server shutdown
        server.addIgnoredErrors(Arrays.asList(MessageConstants.CWWKE1102W_QUIESCE_WARNING,
                MessageConstants.CWWKE1106W_QUIESCE_LISTENERS_NOT_COMPLETE,
                MessageConstants.CWWKE1107W_QUIESCE_WAITING_ON_THREAD));
        // sometimes a port is in use during startup, but is available when tests run - the tests will have issues if
        // the port remains blocked and will generate their own errors - ignore this hiccup during the shutdown checks.
        server.addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0221E_PORT_IN_USE));
        // The code reading the server config sometimes starts before the file is written (it happens rarely)
        // that code will retry and succeed, but the error has already been logged
        // The test framework will still see the error and fail the tests - tell the framework to ignore this error
        server.addIgnoredErrors(Arrays.asList(MessageConstants.CWWKG0014E_CONFIG_PARSER_XML_SYNTAX_ERROR));
        // ignore shutdown timing issues
        server.addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0227E_EXECUTOR_SERVICE_MISSING));
        // ignore potential timing issue with SSL port restart - if the port was ready when tests need it, that's good enough
        server.addIgnoredErrors(Arrays.asList(MessageConstants.SSL_NOT_RESTARTED_PROPERLY));
        // ignore ssl message - runtime retries and can proceed (sometimes) when it can't tests will fail when they don't get the correct response
        server.addIgnoredErrors(Arrays.asList(MessageConstants.CWWKO0801E_UNABLE_TO_INIT_SSL));

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

    public boolean trackerContains(LibertyServer server) {
        return libertyServers.contains(server);
    }

}
