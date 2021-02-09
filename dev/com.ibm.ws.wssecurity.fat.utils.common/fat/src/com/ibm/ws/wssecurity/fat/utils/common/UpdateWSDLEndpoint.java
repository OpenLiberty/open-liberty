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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Vector;

import componenttest.topology.impl.LibertyServer;

public class UpdateWSDLEndpoint {

    private String wsdlFile = "";
    private String serverConfigDir = "";

    public UpdateWSDLEndpoint(LibertyServer server) {
        serverConfigDir = (new File(server.getServerConfigurationPath())).getParent().replace('\\', '/');
    }

    public void updateEndpoint(String filePathUnderServerConfigDir, String oldEndpoint, String newEndpoint) throws Exception {
        // handle the endpoints in the Echo wsdl files in the case of interop
        wsdlFile = serverConfigDir + filePathUnderServerConfigDir;
        updateEndpoint(oldEndpoint, newEndpoint);
    }

    public String getServerConfigDir() {
        return serverConfigDir;
    }

    /*
     * This method copies the old WSDL file to the new WSDL file and updates the port number
     * in the new WSDL file.
     */
    private void updateEndpoint(String oldEndpoint, String newEndpoint) throws Exception {

        BufferedReader ibr = null;
        BufferedWriter obr = null;

        String oneLine = "";

        try {
            ibr = new BufferedReader(new FileReader(wsdlFile));
            Vector<String> vec = new Vector<String>(300, 200);
            while ((oneLine = ibr.readLine()) != null) {
                String strTmp1 = oneLine;
                oneLine = oneLine.replaceAll(oldEndpoint, newEndpoint);
                if (!oneLine.equals(strTmp1)) {
                    System.out.println("<<" + strTmp1);
                    System.out.println(">>" + oneLine);
                }
                vec.addElement(oneLine);
            }

            ibr.close();

            obr = new BufferedWriter(new FileWriter(wsdlFile));
            int iTotal = vec.size();
            String strTmp = "";
            for (int iI = 0; iI < iTotal; iI++) {
                strTmp = vec.elementAt(iI);
                obr.write(strTmp);
                obr.newLine();
            }
            obr.flush();
            obr.close();
        } catch (Exception ex) {
            throw ex;
        } finally {
            try {
                if (obr != null) {
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
}
