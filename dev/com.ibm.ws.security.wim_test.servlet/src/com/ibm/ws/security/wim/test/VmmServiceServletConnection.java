/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.ibm.ws.security.registry.RegistryException;
import com.ibm.wsspi.security.wim.exception.EntityNotFoundException;
import com.ibm.wsspi.security.wim.exception.WIMException;

/**
 * Test abstraction layer. Implement a programmatic UserRegistry test API
 * that drives the real implementation logic via an associated test servlet.
 */
public class VmmServiceServletConnection {
    private static final Class<?> c = VmmServiceServletConnection.class;
    private final Logger logger;
    private final String servletURL;

    public VmmServiceServletConnection(String host, int port) {
        if (host == null || port == 0) {
            throw new IllegalArgumentException("Host (" + host + "is null or port (" + port + ") is zero");
        }
        servletURL = "http://" + host + ":" + port + "/vmmService";
        logger = Logger.getLogger(c.getCanonicalName());
        logger.info("Servlet URL: " + servletURL);
    }

    /**
     * Actually make and parse out the method call.
     *
     * @param methodName
     * @param url
     * @return
     * @throws IOException throw any exceptions encountered, it will be logged by the caller
     */
    private String do_makeServletMethodCall(String methodName, HttpURLConnection con) throws IOException {
        String line = null;
        String resultLine = null;

        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            con.setReadTimeout(600000);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            is = con.getInputStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);

            boolean foundResultLine = false;
            boolean populatedResultLine = false;
            while ((line = br.readLine()) != null) {
                logger.info("Read line: " + line);
                if (foundResultLine && !populatedResultLine) {
                    resultLine = line;
                    populatedResultLine = true;
                }
                if (line.startsWith("Result from method: " + methodName)) {
                    foundResultLine = true;
                }
                if (line.startsWith("getCurrentUserRegistry exception message:")) {
                    foundResultLine = true;
                }
            }
            if (!foundResultLine || resultLine == null) {
                throw new IllegalStateException("Error: expected return line from servlet response not found");
            }
        } catch (SocketTimeoutException e) {
            Thread.dumpStack();
            throw e;
        } finally {
            try {
                if (isr != null)
                    isr.close();
            } catch (IOException ioe) {
                logger.info(ioe.getMessage());
            }
            try {
                if (is != null)
                    is.close();
            } catch (IOException ioe) {
                logger.info(ioe.getMessage());
            }
            try {
                if (br != null)
                    br.close();
            } catch (IOException ioe) {
                logger.info(ioe.getMessage());
            }

            if (con != null)
                con.disconnect();
        }
        logger.info("result line = " + resultLine);
        return resultLine;
    }

    /**
     * Makes the servlet call, and returns the response of the UserRegistry object.
     *
     * @param methodName
     * @param servletRequest
     * @return
     */
    protected String makeServletMethodCall(final String methodName, final String servletRequest) {
        logger.info("START servlet invocation for [" + methodName + "]");
        String result = null;

        try {
            result = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws Exception {
                    URL url = new URL(servletURL + servletRequest);
                    logger.info("Invocation URL=" + url);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    return do_makeServletMethodCall(methodName, con);
                }
            });
        } catch (Exception e) {
            logger.severe("Exception occured while trying to access servlet");
            e.printStackTrace();
            result = e.toString();
        }

        logger.info("END servlet invocation for [" + methodName + "], returning [" + result + "]");
        return result;
    }

    /**
     * @param ret
     * @throws RegistryException
     */
    private void throwIfWIMException(String ret) throws WIMException {
        if (!ret.startsWith(WIMException.class.getName()))
            return;
        String[] split = ret.split(": ", 2);
        String msg = split.length == 2 ? split[1] : split[0];
        throw new WIMException(msg);
    }

    /**
     * @param ret
     * @throws Exception
     */
    private void throwIfEntityNotFoundException(String ret) throws EntityNotFoundException {
        if (!ret.startsWith(EntityNotFoundException.class.getName()))
            return;
        String[] split = ret.split(": ", 2);
        String msg = split.length == 2 ? split[1] : split[0];

        String[] split1 = msg.split(": ", 2);
        String key = split1.length == 2 ? split1[0] : split1[1];
        throw new EntityNotFoundException(key, msg);
    }

    /**
     * @param ret
     * @throws IllegalArgumentException
     */
    private void throwIfIllegalArgumentException(String ret) throws IllegalArgumentException {
        if (!ret.startsWith(IllegalArgumentException.class.getName()))
            return;
        String[] split = ret.split(": ", 2);
        String msg = split.length == 2 ? split[1] : split[0];
        throw new IllegalArgumentException(msg);
    }

    public String makeServletMethodCallWithException(String methodName, String servletRequest) throws WIMException {
        String ret = makeServletMethodCall(methodName, servletRequest);
        throwIfEntityNotFoundException(ret);
        throwIfWIMException(ret);
        throwIfIllegalArgumentException(ret);
        if (ret.equals("null")) {
            return null;
        } else {
            return ret;
        }
    }

    protected String makeServletMethodCallWithExceptions(String methodName, String servletRequest) throws WIMException {
        String ret = makeServletMethodCall(methodName, servletRequest);
        throwIfWIMException(ret);
        //throwIfEntryNotFoundException(ret);
        throwIfIllegalArgumentException(ret);
        return ret;
    }

    /**
     * @param methodName
     * @param str
     * @return
     */
    private boolean convertToBoolean(String methodName, String str) {
        if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(str);
        } else {
            throw new IllegalStateException(methodName + "expected either 'true' or 'false', but was: " + str);
        }
    }

    /**
     * @param methodName
     * @param str
     * @return
     */
    public static List<String> convertToList(String methodName, String str) {
        List<String> list = new ArrayList<String>();
        if (str.startsWith("[") && str.endsWith("]")) {
            if (str.length() == 2) {
                return list;
            } else {
                String[] contents = str.substring(1, str.length() - 1).split(", ");
                for (String entry : contents) {
                    list.add(entry);
                }
                return list;
            }
        } else {
            throw new IllegalStateException(methodName + "expected [...], but was: " + str);
        }
    }

    /**
     * @param methodName
     * @param str
     * @return
     */
    private Map<String, String> convertToMap(String methodName, String str) {
        Map<String, String> props = new HashMap<String, String>();
        if (str.startsWith("{") && str.endsWith("}")) {
            if (str.length() == 2) {
                return props;
            } else {
                String[] contents = str.substring(1, str.length() - 1).split(", ");
                for (String entry : contents) {
                    StringTokenizer strTokan = new StringTokenizer(entry, "=");
                    while (strTokan.countTokens() == 2) {
                        props.put((String) strTokan.nextElement(), (String) strTokan.nextElement());
                    }
                }
                return props;
            }
        } else {
            throw new IllegalStateException(methodName + " expected {srt1=str2, ... }, but was: " + str);
        }
    }

    /**
     * @param userSecurityName
     * @return
     */
    public static String encodeSpaces(String userSecurityName) {
        return userSecurityName.replaceAll(" ", "%20");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.wim.ProfileServiceLite#get(com.ibm.wsspi.security.wim.model.Root)
     */
    public Map<String, String> getUser(String uniqueName) throws WIMException, RemoteException {
        String methodName = "getUser";
        String servletRequest = "?method=" + methodName +
                                "&uniqueName=" + encodeSpaces(uniqueName);
        String servletResponse = makeServletMethodCallWithException(methodName, servletRequest);
        return convertToMap(methodName, servletResponse);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.wim.ProfileServiceLite#search(com.ibm.wsspi.security.wim.model.Root)
     */
    public List<String> search(String searchExp, List<String> returnPros) throws WIMException, RemoteException {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.wim.ProfileServiceLite#login(com.ibm.wsspi.security.wim.model.Root)
     */
    public String login(String userName, String password) throws WIMException, RemoteException {
        String methodName = "login";
        String servletRequest = "?method=" + methodName +
                                "&userName=" + encodeSpaces(userName) +
                                "&password=" + encodeSpaces(password);
        return makeServletMethodCallWithException(methodName, servletRequest);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.wim.ProfileServiceLite#delete(com.ibm.wsspi.security.wim.model.Root)
     */
    public String deleteUser(String uniqueName) throws WIMException, RemoteException {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.wim.ProfileServiceLite#create(com.ibm.wsspi.security.wim.model.Root)
     */
    public String createUser(String uid, String cn, String sn, String password, String parent) throws WIMException, RemoteException {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.websphere.security.wim.ProfileServiceLite#update(com.ibm.wsspi.security.wim.model.Root)
     */
    public String updateUser(String uniqueName, String uidToUpdate, String cnToUpdate, String snToUpdate) throws WIMException, RemoteException {
        return null;
    }

    public String ping() throws WIMException, RemoteException {
        String methodName = "ping";
        String servletRequest = "?method=" + methodName;
        return makeServletMethodCallWithException(methodName, servletRequest);
    }
}
