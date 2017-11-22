/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.suite;

import java.util.logging.Logger;

import com.ibm.ws.fat.util.SharedServer;

public class ShutDownSharedServer extends SharedServer {

    private boolean shutdownAfterTest = true;
    private static final Logger LOG = Logger.getLogger(SharedServer.class.getName());

    public ShutDownSharedServer(String serverName) {
        super(serverName);

    }

    public void setAutoShutdown(boolean shutDown) {
        shutdownAfterTest = shutDown;
    }

    @Override
    protected void after() {
        if (shutdownAfterTest && getLibertyServer().isStarted()) {
            try {
                getLibertyServer().stopServer();
            } catch (Exception e) {
                throw new RuntimeException(e); //TODO something better here.
            }
        }
    }

}
