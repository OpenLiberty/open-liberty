/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.database;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.derby.drda.NetworkServerControl;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Provides methods for starting and stopping Derby Network.
 * The server is checked a number of times to be sure it has started,
 * otherwise the exception returned from the ping is thrown.
 */
public class DerbyNetworkUtilities {
    private static final Class<?> c = DerbyNetworkUtilities.class;

    static final int NUMBER_OF_PINGS = 10;
    static final int SLEEP_BETWEEN_PINGS = 10000;
    // TODO serverName and portNumber are hard coded,
    // but in the future should be taken from bootstrapping.properties if specified
    static final String serverName = "localhost";
    static final int portNumber = 1527;

    static public void startDerbyNetwork() throws UnknownHostException, Exception {
        final String m = "startDerbyNetwork";
        NetworkServerControl serverControl = new NetworkServerControl(InetAddress.getByName(serverName), portNumber);
        serverControl.start(new PrintWriter(System.out));
        for (int i = 1; i <= NUMBER_OF_PINGS; i++) {
            try {
                Log.info(c, m, "Attempt " + i + " to see if Derby Network server started");
                serverControl.ping();
                break;
            } catch (Exception ex) {
                if (i == NUMBER_OF_PINGS) {
                    Log.info(c, m, "Derby Network server failed to start");
                    Log.error(c, m, ex);
                    throw ex;
                } else {
                    Log.warning(c, "Attempt " + i + " failed due to exception: " + System.lineSeparator() + ex.getMessage());
                    Thread.sleep(SLEEP_BETWEEN_PINGS);
                }
            }
        }
    }

    static public void stopDerbyNetwork() throws Exception {
        NetworkServerControl serverControl = new NetworkServerControl(InetAddress.getByName(serverName), portNumber);
        serverControl.shutdown();
    }

}
