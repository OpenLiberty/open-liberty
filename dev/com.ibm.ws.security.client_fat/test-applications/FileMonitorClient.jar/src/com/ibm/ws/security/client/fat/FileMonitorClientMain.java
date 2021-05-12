/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.client.fat;


//import java.io.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;


//import javax.naming.*;
//import javax.servlet.*;
//import javax.servlet.http.*;
//import java.rmi.RemoteException;


public class FileMonitorClientMain {

 
    /*
     * Want to update client keystore file
     */
    public void doFileModification(String[] args) {
     
        try {
          System.out.println("sleep for 10 secs ");
          Thread.sleep(10000);
          String toFile = args[0];
          String fromFile = args[1];
          System.out.println("toFile is: " + toFile);
          System.out.println("fromFile is: " + fromFile);
          File toPath = new File(toFile);
          File fromPath = new File(fromFile);
          copyFileUsingFileChannels(fromPath, toPath);
          System.out.println("sleep for 10 secs ");
          Thread.sleep(10000);
          System.out.println("test is complete");
        } catch (Throwable t) {
            System.out.println("throwable: " + t);
            t.printStackTrace();
        } finally {
            
        }
    }
    private static void copyFileUsingFileChannels(File source, File dest)
    	        throws IOException {
    	    FileChannel inputChannel = null;
    	    FileChannel outputChannel = null;
    	    try {
    	        inputChannel = new FileInputStream(source).getChannel();
    	        outputChannel = new FileOutputStream(dest).getChannel();
    	        System.out.println("copying files using channel method");
    	        outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            } catch (Throwable t) {
                System.out.println("throwable: " + t);
                t.printStackTrace();     
            } finally {
    	        inputChannel.close();
    	        outputChannel.close();
    	    }
    }
    
    /**
     * Entry point to the program used by the J2EE Application Client Container
     */
    public static void main(String[] args) {
        
        FileMonitorClientMain FMclient = new FileMonitorClientMain();
        FMclient.doFileModification(args);
    }
}
