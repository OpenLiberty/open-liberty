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

public class UpdateWSDLPortNum {

    private String oldWsdlFile = "";
    private String newWsdlFile = "";

    public UpdateWSDLPortNum(String oldFile, String newFile) {
        oldWsdlFile = oldFile;
        newWsdlFile = newFile;
    }

    /*
     * This method copies the old WSDL file to the new WSDL file and updates the port number
     * in the new WSDL file.
     */
    public void updatePortNum(String oldPort, String newPort) throws Exception {

        BufferedReader ibr = null;
        BufferedWriter obr = null;

        String oneLine = "";

        try {
            ibr = new BufferedReader(new FileReader(oldWsdlFile));
            obr = new BufferedWriter(new FileWriter(newWsdlFile));
            // Copy old WSDL to new WSDL and update port number in new WSDL.
            while ((oneLine = ibr.readLine()) != null) {
                if (oneLine.contains(oldPort)) {
                    oneLine = oneLine.replaceAll(oldPort, newPort);
                }
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

    public void removeWSDLFile() {

        File newFile = new File(newWsdlFile);
        newFile.delete();

        return;

    }

}
