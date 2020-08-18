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

package com.ibm.ws.security.client.fat.java2;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import com.ibm.ws.security.client.fat.Java2Test;

public class Java2App {

    /**
     * Perform the requested operation. The first arg is the operation name.
     * Currently supported operations are: 
     *     setProperty, getProperty, readFile, writeFile
     * 
     * @param args
     */
    public static void main(String args[]) {
        System.out.println("Start of client Java2Test application.");
        String operation = null;
        if (args.length > 0) {
            operation = args[0];
            System.out.println("operation: " + operation);
        } else {
            System.out.println("No operation suplied, returning");
        }
        try {
            if (operation.equals(Java2Test.READ_FILE_OPERATION)) {
                String fileName = args[1];
                System.out.println("reading file: " + fileName);
                File file = new File(fileName);
                FileReader rdr = new FileReader(file);
                char[] chars = new char[(int) file.length()];
                rdr.read(chars);
                System.out.println("file read");
                rdr.close();
            } else if (operation.equals(Java2Test.WRITE_FILE_OPERATION)) {
                String fileName = args[1];
                System.out.println("writing file: " + fileName);
                File file = new File(fileName);
                FileWriter writer = new FileWriter(file);
                writer.write(" bob", (int) file.length(), 4);
                System.out.println("file written");
                writer.close();                
            } else if (operation.equals(Java2Test.GET_PROPERTY_OPERATION)) {
                System.out.println("getting property bob");
                String value = System.getProperty("bob");
                System.out.println("property bob value is: " + value);
            } else if (operation.equals(Java2Test.SET_PROPERTY_OPERATION)) {
                System.out.println("setting property bob");
                System.setProperty("bob", "phil");
                System.out.println("property bob set to: phil");
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage().toLowerCase());
        }
        System.out.println("End of client Java2Test application.");
    }
}