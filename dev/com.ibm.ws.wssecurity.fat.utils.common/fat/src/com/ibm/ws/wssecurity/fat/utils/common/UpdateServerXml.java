/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.utils.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import componenttest.topology.impl.LibertyServer;

public class UpdateServerXml {

    static private final Class<?> thisClass = UpdateServerXml.class;

    /*
     * This method copied contents of fromFile to to File. fromFile - new
     * server.xml file toFile - old server.xml
     */
    public static void copyFile(String fromFile, String toFile) throws Exception {

        BufferedReader ibr = null;
        BufferedWriter obr = null;

        String oneLine = "";

        try {
            ibr = new BufferedReader(new FileReader(fromFile));
            obr = new BufferedWriter(new FileWriter(toFile));
            // Copy fromFile to toFile
            while ((oneLine = ibr.readLine()) != null) {
                obr.write(oneLine);
                obr.newLine();
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            try {
                if (obr != null) {
                    obr.flush();
                    obr.close();
                }
                if (ibr != null) {
                    ibr.close();
                }
            } catch (Exception e) {
                throw e;
            }
        }

        return;
    }

    public static void reconfigServer(LibertyServer server, String copyFromFile) throws Exception {
        CommonTests.server = server;
        CommonTests.reconfigServer(copyFromFile, "CallFromUpdateServerXml");

    }

    public static void reconfigServer(LibertyServer server, String copyFromFile, String methodName) throws Exception {
        CommonTests.server = server;
        CommonTests.reconfigServer(copyFromFile, methodName);
    }
}
