/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package com.ibm.ws.ui.internal.v1.utils;

import com.google.gson.Gson;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.rest.handler.RESTRequest;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import java.lang.Exception;
import java.util.Set;

/**
 * This class is designed to hold utility methods that different parts of the server side UI will need to use.
 */
public class Utils {
    private static final TraceComponent tc = Tr.register(Utils.class);
    static final MessageDigest messagedigest;

    static {
        try {
            messagedigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            //should not happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Encodes the specified String using URL encoding.
     *
     * @param toEncode The String to URL encode
     * @return The URL encoded String
     */
    @Trivial
    public static final String urlEncode(final String toEncode) {
        if (toEncode == null) {
            return null;
        }
        try {
            return URLEncoder.encode(toEncode, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Let this FFDC because we should never get here
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Unexpected UnsupportedEncodingException", e);
            }
            throw new IllegalStateException("Encountered a JVM with no UTF-8 support!");
        }
    }

    /**
     * Returns a deterministic already encoded path since Jakarta EE9 switch the default value of decodeUrlPlusSign we can no longer tell if getPath() is
     * returning a string that is encoded or decoded.
     *
     * @param request The request to get the path from
     * @return The URL encoded String
     */
    public static String getPath(final RESTRequest request) {
        String path = request.getURI();

        try {
            //take off context path
            String contextPath = request.getContextPath();
            path = path.substring(path.indexOf(contextPath) + contextPath.length());
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception while trying to get urlencoded path from request.", e);
            }
            return request.getPath();
        }
        return path;
    }

    /**
     * This method returns a URL object for the supplied url name.
     *
     * @param url The String urlName that we should turn into a URL Object.
     * @return The URL object.
     * @throws MalformedURLException
     */
    @FFDCIgnore(PrivilegedActionException.class)
    @Trivial
    public static final URL getURL(final String urlName) throws MalformedURLException {
        // we have to get the URL in a doPriv block.
        try {
            URL url = AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                @Trivial
                @Override
                public URL run() throws MalformedURLException {
                    return new URL(urlName);
                }
            });
            return url;
        } catch (PrivilegedActionException e) {
            throw (MalformedURLException) e.getCause();
        }
    }

    /**
     * Generates the md5 checksum of the given string
     *
     * @param str The input string
     * @return The MD5 checksum of the given string.
     * @throws UnsupportedEncodingException
     */
    public synchronized static String getMD5String(String str) {
        byte[] hash;
        try {
            hash = messagedigest.digest(str.getBytes("UTF-8"));
        } catch (Exception e) {
            // Let this FFDC because we should never get here
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Unexpected Exception", e);
            }
            throw new IllegalStateException("Encountered a JVM with no UTF-8 support!");
        }
        StringBuilder sb = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    

    /**
     * This method validates whether the input string is a valid JSON or not.
     * @param inputString, Input string
     * @param prefix, Prefix string to be trimmed from input string
     * @return Boolean, true if input string is valid JSON.
     */
    public static boolean isValidJsonString(String inputString, String prefix) {
        if(!prefix.equals("")) {
            inputString = inputString.replace(prefix, "");
        }

        return isValidJsonString(inputString);
    }
    

    /**
     * This method validates whether the input string is a valid JSON or not
     * @param inputString The input string
     * @return Boolean, true if input string is valid JSON.
     */
    public static boolean isValidJsonString(String inputString) {
        boolean isValid = false;

        try {
            Gson gson = new Gson();
            gson.fromJson(inputString, Object.class);
            isValid = true;
        } catch (Exception e) {
            isValid = false;
        }

        return isValid;
    }

    /**
     * This method validates whether the input JSON string format is a valid JavaBath data or not
     * @param inputString The input string
     * @return Boolean, true if input string is valid JavaBatch data.
     */
    public static boolean isValidJavaBatchData(String inputString) {
        boolean isValidData = false;

        try {
            JsonElement jsonElement = JsonParser.parseString(inputString);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "jsonElement ", jsonElement);
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            Set<String>	keys = jsonObject.keySet();
            if (keys.size() != 2)
                throw new Exception("Expected jobInstanceGrid & jobInstancesKey properties only");

            // get jobInstanceGrid
            JsonElement jobInstanceGridElem = jsonObject.get("jobInstanceGrid");
            JsonObject jobInstanceGrid = jobInstanceGridElem.getAsJsonObject();
            // get jobInstanceGrid column
            JsonElement columnElem = jobInstanceGrid.get("column");
            JsonArray columns = columnElem.getAsJsonArray();
            if (columns.size() != 9)
                throw new Exception("Expected 9 columns for jobInstanceGrid");
            for (int i = 0; i < columns.size(); i++) {
                JsonObject col = (JsonObject) columns.get(i);
                JsonElement idElem = col.get("id");
                String id = idElem.getAsString();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "id ", id);
                }
                if (i == 0 && id.equals("executionsDropDown") ||
                    i == 1 && id.equals("jobName") ||
                    i == 2 && id.equals("instanceId") ||
                    i == 3 && id.equals("appName") ||
                    i == 4 && id.equals("submitter") ||
                    i == 5 && id.equals("lastUpdatedTime") ||
                    i == 6 && id.equals("instanceState") ||
                    i == 7 && id.equals("actions") ||
                    i == 8 && id.equals("log")) {
                    // good id
                } else {
                    throw new Exception("Invalid id");
                }

                JsonElement widthElem = col.get("width");
                String width = widthElem.getAsString();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "width ", width);
                }
                // match only number and ends with %
                String regex = "[0-9]+%$";
                if (!width.matches(regex))
                    throw new Exception("Invalid width value");
            }
            // get jobInstanceGrid hidableColumn
            JsonElement jobInstanceHidableColumnElem = jobInstanceGrid.get("hideableColumnInfo");
            JsonArray hideableColumnInfo = jobInstanceHidableColumnElem.getAsJsonArray();
            if (hideableColumnInfo.size() > 2)
                throw new Exception("Invalid number of hideableColumnInfo");
            else if (hideableColumnInfo.size() == 1) {
                JsonElement hidableColumn = hideableColumnInfo.get(0);
                String hidableColumnStr1 = hidableColumn.getAsString();
                if ((hidableColumnStr1.equals("JESJobName") ||
                     hidableColumnStr1.equals("JESJobId"))) {
                    // valid hidableColumn
                } else {
                    throw new Exception("Invalid hidableColumnInfo headers");
                }
            } else if (hideableColumnInfo.size() == 2) {
                JsonElement hidableColumn1 = hideableColumnInfo.get(0);
                String hidableColumnStr1 = hidableColumn1.getAsString();
                JsonElement hidableColumn2 = hideableColumnInfo.get(1);
                String hidableColumnStr2 = hidableColumn2.getAsString();
                if ((hidableColumnStr1.equals("JESJobName") &&
                     hidableColumnStr2.equals("JESJobId"))
                    ||
                    (hidableColumnStr1.equals("JESJobId") &&
                     hidableColumnStr2.equals("JESJobName"))
                    ) {
                    // valid hidableColumn
                } else {
                    throw new Exception("Invalid hidableColumnInfo headers");
                }
            }

            // get jobInstancesKey
            JsonElement jobInstancesKeyElem = jsonObject.get("jobInstancesKey");
            String jobInstancesKey = jobInstancesKeyElem.getAsString();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "jobInstancesKey ", jobInstancesKey);
            }
            if (!jobInstancesKey.startsWith("{\"querySize\"")) {
                throw new Exception("jobInstancesKey key is invalid");
            } else {
                int index = jobInstancesKey.indexOf(":") + 1;
                String num = jobInstancesKey.substring(index, jobInstancesKey.indexOf("}"));
                String regex = "[0-9]+";
                if (!num.matches(regex))
                    throw new Exception("jobInstancesKey value is invalid");
            }

            isValidData = true;

        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Not a valid format: " + e.getMessage());
            }
        }

        return isValidData;
    }
}
