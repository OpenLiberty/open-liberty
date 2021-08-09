/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.fat.common.CommonMessageTools;

public class DerbyUtils {

    private final static Class<?> thisClass = DerbyUtils.class;

    public static CommonMessageTools msgUtils = new CommonMessageTools();

    public static void setupDerbyEntries(String httpString, Integer defaultPort) throws Exception {

        HttpURLConnection con = null;
        try {
            msgUtils.printMethodName("setupDerbyEntries");
            Log.info(thisClass, "setupDerbyEntries", "Create DataBases through the server");
            URL setupURL = AutomationTools.getNewUrl(httpString + "/oAuth20DerbySetup?port=" + defaultPort + "&schemaName=OAuthDBSchema");
            Log.info(thisClass, "setupDerbyEntries", "setupURL: " + setupURL);
            con = (HttpURLConnection) setupURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(thisClass, "runInJDBCFATServlet", line);
            }

            con.disconnect();

        } catch (Exception e) {

            Log.info(thisClass, "setupDerbyEntries", "Exception occurred: " + e.toString());
            Log.error(thisClass, "setupDerbyEntries", e, "Exception occurred");
            System.err.println("Exception: " + e);
            if (con != null) {
                con.disconnect();
            }
            throw e;
        }

    }

    public static void cleanupDerbyEntries(String httpString, Integer defaultPort) throws Exception {

        HttpURLConnection con = null;
        try {
            msgUtils.printMethodName("cleanupDerbyEntries");
            Log.info(thisClass, "cleanupDerbyEntries", "Drop DataBases through the server");
            URL setupURL = AutomationTools.getNewUrl(httpString + "/oAuth20DerbySetup?port=" + defaultPort + "&cleanup=true");
            Log.info(thisClass, "setupDerbyEntries", "cleanupURL: " + setupURL);
            con = (HttpURLConnection) setupURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(thisClass, "runInJDBCFATServlet", line);
            }

            con.disconnect();

        } catch (Exception e) {

            Log.info(thisClass, "cleanupDerbyEntries", "Exception occurred: " + e.toString());
            Log.error(thisClass, "cleanupDerbyEntries", e, "Exception occurred");
            System.err.println("Exception: " + e);
            if (con != null) {
                con.disconnect();
            }
            // throw e;
        }

    }

    public static void addDerbyEntry(String httpString, Integer defaultPort, String clientID, String secret,
            String compID, String salt, String alg) throws Exception {
        String METHOD = "addDerbyEntry";
        HttpURLConnection con = null;
        try {
            msgUtils.printMethodName(METHOD);
            Log.info(thisClass, METHOD, "Add a new client " + clientID);
            // Sending the secret as a parameter for test purposes only, so users can be manually added to the database and then
            // read via the Database Store
            URL setupURL = AutomationTools.getNewUrl(
                    httpString + "/oAuth20DerbySetup?port=" + defaultPort + "&schemaName=OAuthDBSchema&addClient=true"
                            + "&clientID=" + clientID + "&secret=" + secret + "&compID=" + compID + (salt != null ? ("&checkSalt=" + salt) : "")
                            + (alg != null ? ("&checkAlgorithm=" + alg) : ""));
            Log.info(thisClass, METHOD, "setupURL: " + setupURL);
            con = (HttpURLConnection) setupURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(thisClass, METHOD, line);
            }

            con.disconnect();

        } catch (Exception e) {

            Log.info(thisClass, METHOD, "Exception occurred: " + e.toString());
            Log.error(thisClass, METHOD, e, "Exception occurred");
            System.err.println("Exception: " + e);
            if (con != null) {
                con.disconnect();
            }
            throw e;
        }

    }

    public static String checkSecretType(String httpString, Integer defaultPort, String clientID, String compID)
            throws Exception {
        return checkDerbyEntry(httpString, defaultPort, clientID, compID, "checkSecret");
    }

    public static String checkSalt(String httpString, Integer defaultPort, String clientID, String compID)
            throws Exception {
        return checkDerbyEntry(httpString, defaultPort, clientID, compID, "checkSalt");
    }

    public static String checkAlgorithm(String httpString, Integer defaultPort, String clientID, String compID)
            throws Exception {
        return checkDerbyEntry(httpString, defaultPort, clientID, compID, "checkAlgorithm");
    }

    public static String checkIteration(String httpString, Integer defaultPort, String clientID, String compID)
            throws Exception {
        return checkDerbyEntry(httpString, defaultPort, clientID, compID, "checkIteration");
    }

    public static String checkAccessToken(String httpString, Integer defaultPort, String clientID, String encodingType)
            throws Exception {
        return checkDerbyEntry(httpString, defaultPort, clientID, null, "checkAccessToken_" + encodingType);
    }

    public static String checkDerbyEntry(String httpString, Integer defaultPort, String clientID, String compID, String checkType)
            throws Exception {

        String METHOD = "checkDerbyEntry";
        HttpURLConnection con = null;

        String msg = null;
        try {
            msgUtils.printMethodName(METHOD);
            Log.info(thisClass, METHOD, "Check the " + checkType + " on client " + clientID);
            URL setupURL = AutomationTools.getNewUrl(
                    httpString + "/oAuth20DerbySetup?port=" + defaultPort + "&schemaName=OAuthDBSchema"
                            + "&clientID=" + clientID + (compID != null ? ("&compID=" + compID) : "") + "&" + checkType + "=true");
            Log.info(thisClass, METHOD, "setupURL: " + setupURL);
            con = (HttpURLConnection) setupURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(thisClass, METHOD, line);
                msg = line;
            }

            con.disconnect();

        } catch (Exception e) {

            Log.info(thisClass, METHOD, "Exception occurred: " + e.toString());
            Log.error(thisClass, METHOD, e, "Exception occurred");
            System.err.println("Exception: " + e);
            if (con != null) {
                con.disconnect();
            }
            throw e;

        }

        return msg;

    }

    public static void clearClientEntries(String httpString, Integer defaultPort) throws Exception {
    	String METHOD = "clearClientEntries";
        HttpURLConnection con = null;
        try {
            msgUtils.printMethodName(METHOD);
            Log.info(thisClass, METHOD, "Remove client entries from DB and add them again.");
            URL setupURL = AutomationTools.getNewUrl(httpString + "/oAuth20DerbySetup?port=" + defaultPort + "&clearClients=true" + "&schemaName=OAuthDBSchema");
            Log.info(thisClass, METHOD, "cleanupURL: " + setupURL);
            con = (HttpURLConnection) setupURL.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();

            // Send output from servlet to console output
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
                Log.info(thisClass, METHOD, line);
            }

            con.disconnect();

        } catch (Exception e) {

            Log.info(thisClass, METHOD, "Exception occurred: " + e.toString());
            Log.error(thisClass, METHOD, e, "Exception occurred");
            System.err.println("Exception: " + e);
            if (con != null) {
                con.disconnect();
            }
            // throw e;
        }

    }
}
