/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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
