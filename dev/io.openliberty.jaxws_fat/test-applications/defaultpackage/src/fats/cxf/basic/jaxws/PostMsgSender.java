/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package fats.cxf.basic.jaxws;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Migrated from tWAS based com.ibm.ws.jaxws_fat jaxws/defaultpackage/** bucket
 */
public class PostMsgSender {

    public static int responseCode = -1; // the response code from the http response.
    public static String response = null;
    private static final int WAITTIME = 1000; // Wait 1 seconds for each attempt

    /*
     * post to a url. Returns the response as a string.
     * Some of the http headers are hardcoded, and the message is limited to 64k.
     * url - the url
     * postmsg - the message
     * soapaction - the soapaction to send, null for none.
     * timeoutSec - how long to wait for a response before giving up.
     * return a string containing "timeout" and perhaps some explanation.
     * ignoreContents - just return "true" or "false" if we could post or not.
     *
     */
    public static String postToURL(String url, String postmsg, String soapaction,
                                   int timeoutSec, boolean ignoreContents) throws Exception {

        // clear response variables
        responseCode = -1;
        response = null;

        URL u = null;
        HttpURLConnection h = null;

        boolean gotresponse = false;
        byte[] buf = null;
        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException("malformed url");
        }
        int i = 0;
        System.out.println("trying: " + url);
        long initialTime = java.lang.System.currentTimeMillis();
        long endtime = initialTime + (1000 * timeoutSec);

        // write the message
        do {
            try {
                h = (HttpURLConnection) u.openConnection();
                h.setDoOutput(true);
                //h.setRequestProperty("Accept", "application/soap+xml,multipart/related,text/*");
                h.setRequestProperty("Accept", "application/soap+xml,multipart/related,text/*");
                if (soapaction != null) {
                    h.setRequestProperty("SOAPAction", soapaction);
                }
                // h.setRequestProperty("Content-Type","application/xml; charset=UTF-8");
                h.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
                h.setRequestProperty("Content-Length", String.valueOf(postmsg.length()));
                h.connect();
                java.io.DataOutputStream o = new java.io.DataOutputStream(h.getOutputStream());
                o.writeBytes(postmsg);
                o.flush();

                responseCode = h.getResponseCode();

            } catch (IOException e) {
                i++;
                System.out.println("waiting for server: " + url);
                try {
                    Thread.sleep(WAITTIME);
                } catch (InterruptedException x) {
                }
                continue;
            }
            gotresponse = true;
            break;

        } while (System.currentTimeMillis() < endtime);

        if (gotresponse) {

            if (responseCode < 200 || responseCode > 300) {
                System.out.println("got error response: " + responseCode);
                gotresponse = false;
            } else {
                System.out.println("got response:" + responseCode);
            }

        } else {
            System.out.println("no response, timed out on url: " + url);
            h.disconnect();
            response = "timeout posting message";
            return response;
        }

        if (ignoreContents) {
            h.disconnect();
            if (gotresponse) {
                response = "true";
            } else {
                response = "false";
            }
            return response;
        }

        // otherwise, go get the contents
        try {
            /*
             * Map m = h.getHeaderFields();
             * System.out.println(m.toString());
             */

            // if the url returned an error code,
            // attempting to read the input stream will throw an exception.
            // In that case, we need to read the error stream instead.
            InputStream is = null;
            try {
                is = h.getInputStream();
            } catch (IOException e) {
                is = h.getErrorStream();
            }

            buf = new byte[64000];
            int bytesread = 0;
            int totalbytesread = 0;
            boolean timesUp = false;

            do {
                bytesread = is.read(buf);
                totalbytesread = totalbytesread + bytesread;
                timesUp = (System.currentTimeMillis() > endtime);
            } while (bytesread > -1 && !timesUp);

            if (timesUp) {
                System.out.println("timed out on url: " + url);
                h.disconnect();
                response = "timeout waiting for response";
                return response;
            }

            h.disconnect();
            response = new String(buf, 0, totalbytesread + 1); // how we convert byte[] to  String...
            return response;

        } catch (IOException e) {
            // e.printStackTrace();
            System.out.println("caught io exception, message =" + e.getMessage());
            response = null;
            return response;
        }
    }

}
