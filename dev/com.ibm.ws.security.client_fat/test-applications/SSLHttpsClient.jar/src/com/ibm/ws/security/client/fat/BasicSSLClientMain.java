/*******************************************************************************
 * Copyright (c) 2001, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.client.fat;


import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

//import java.io.*;
//import javax.naming.*;
//import javax.servlet.*;
//import javax.servlet.http.*;
//import java.rmi.RemoteException;


public class BasicSSLClientMain {

 
    /*
     * Want to make a https connection to another server
     */
    public void doSSLConnection() {
    	HttpURLConnection con = null;
        try {
            HostnameVerifier hv = new HostnameVerifier() {
                @Override
                public boolean verify(String urlHostName, SSLSession session) {
                    System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
            String  httpsPortString = System.getProperty("ServerSecurePort");
            int httpsPort = Integer.parseInt(httpsPortString);
            URL url = new URL("https://localhost:" + httpsPort + "/basicauth/SimpleServlet");
            con = (HttpsURLConnection) url.openConnection();
            con.setReadTimeout(30000);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("POST");
            OutputStream os = con.getOutputStream();
            final int testLen = 1024768;
            for (int i = 0; i < testLen; i++) {
                os.write('a');
            }
            InputStream is = con.getInputStream();
            byte[] data = new byte[testLen];
            int offset = 0;
            do {
                int rc = is.read(data, offset, testLen - offset);
                if (-1 == rc) {
                    break;
                }
                offset += rc;
            } while (offset < testLen);
            is.close();
            System.out.println("Read " + offset + " " + new String(data, 0, offset));
            for (int i = 0; i < testLen; i++) {
                System.out.println("expected 'a' but was " + data[i]);
            }
            con.disconnect();
            con = null;
        } catch (Throwable t) {
            System.out.println("throwable: " + t);
        } finally {
            if (null != con) {
                con.disconnect();
            }
        }
    }

    
    /**
     * Entry point to the program used by the J2EE Application Client Container
     */
    public static void main(String[] args) {
        
        BasicSSLClientMain SSLclient = new BasicSSLClientMain();
        SSLclient.doSSLConnection();
    }
}
