/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.utils.common;

import java.security.Security;
import java.util.HashMap;

import componenttest.topology.impl.LibertyServer;

public class SharedTools {
    static HashMap<String, Object> marks = new HashMap<String, Object>();

    static private final Class<?> thisClass = SharedTools.class;

    public static void fixProviderOrder(String callerName) throws Exception {

        /* handle case where IBMCERT isn't in the list ... */

        int IBMCert = 0;
        int SUN = 0;

        System.out.println("Start printing " + callerName + " providers");
        try {
            java.security.Provider p[] = Security.getProviders();
            for (int i = 0; i < p.length; i++) {
                System.out.println(p[i]);
                if (p[i].toString().contains("IBMCertPath")) {
                    IBMCert = i;
                }
                if (p[i].toString().contains("SUN version")) {
                    SUN = i;
                }
            }

            System.out.println("Finished printing " + callerName + "  providers");
            System.out.println("Start Updating Order");

            if (SUN == 0) {
                System.out.println("NO SUN included - no update needed");
            } else {
                if (SUN < IBMCert) {
                    Security.removeProvider(p[IBMCert].getName());
                    Security.insertProviderAt(p[IBMCert], SUN + 1);
                    System.out.println("Finished Updating Order");

                    System.out.println("Start printing updated " + callerName + "  providers");
                    java.security.Provider pu[] = Security.getProviders();
                    for (int j = 0; j < pu.length; j++) {
                        System.out.println(pu[j]);
                    }
                    System.out.println("Finished printing updated " + callerName + "  providers");
                } else {
                    System.out.println("IBM Cert is already before SUN - no update needed");

                }
            }
        } catch (Exception e) {

        }

    }

    // This work on message.log only.
    // To check other files, we  need to enhance
    // LibertyServer does have parameters for other log files
    static public void setMessageLogMarker(LibertyServer server) { // The server, This can be null if newly start

        if (server != null) {
            try {
                server.setMarkToEndOfLog(); // Default log file is message log
            } catch (Exception e) {

            }
        }
    }

    //
    //  This work on default log file: logs/message.log
    //
    static public String waitForMessageInLog(LibertyServer server,
                                             String... strMessages) {
        return waitForMessageInLog(server, 10, strMessages); // wait time default 10 seconds
    }

    //
    //  This work on default log file: logs/message.log
    //
    static public String waitForMessageInLog(LibertyServer server,
                                             int iWaitSeconds, // default 10
                                             String... strMessages) { // the string to be searched from previous mark
        String noUpdateMsg = null;
        for (String strMessage : strMessages) {
            server.waitForStringInLogUsingMark(strMessage, iWaitSeconds * 1000);
        }
        return noUpdateMsg;
    }

    //issue 23730 12/2022 removed installCallbackHandler() and unInstallCallbackHandler() from SharedTools.java
    //since they are no longer applicable in OL wsscxf FAT where ShrinkHelper with bnd.bnd to handle CBH installation
    //and server.deleteFileFromLibertyInstallRoot() to handle CBH uninstallation

}
