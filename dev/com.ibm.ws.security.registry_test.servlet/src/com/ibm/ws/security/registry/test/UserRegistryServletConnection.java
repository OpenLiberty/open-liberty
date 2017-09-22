/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.registry.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.ibm.ws.security.registry.CertificateMapFailedException;
import com.ibm.ws.security.registry.CertificateMapNotSupportedException;
import com.ibm.ws.security.registry.EntryNotFoundException;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.SearchResult;
import com.ibm.ws.security.registry.UserRegistry;

/**
 * Test abstraction layer. Implement a programmatic UserRegistry test API
 * that drives the real implementation logic via an associated test servlet.
 */
public class UserRegistryServletConnection implements UserRegistry {
    private static final Class<?> c = UserRegistryServletConnection.class;
    private final Logger logger;
    private final String servletURL;

    public UserRegistryServletConnection(String host, int port) {
        if (host == null || port == 0) {
            throw new IllegalArgumentException("Host (" + host + "is null or port (" + port + ") is zero");
        }
        servletURL = "http://" + host + ":" + port + "/userRegistry";
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
    private void throwIfRegistryException(String ret) throws RegistryException {
        if (!ret.startsWith(RegistryException.class.getName()))
            return;
        String[] split = ret.split(": ", 2);
        String msg = split.length == 2 ? split[1] : split[0];
        throw new RegistryException(msg);
    }

    /**
     * @param ret
     * @throws EntryNotFoundException
     */
    private void throwIfEntryNotFoundException(String ret) throws EntryNotFoundException {
        if (!ret.startsWith(EntryNotFoundException.class.getName()))
            return;
        String[] split = ret.split(": ", 2);
        String msg = split.length == 2 ? split[1] : split[0];
        throw new EntryNotFoundException(msg);
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

    protected String makeServletMethodCallWithException(String methodName, String servletRequest) throws RegistryException {
        String ret = makeServletMethodCall(methodName, servletRequest);
        throwIfRegistryException(ret);
        throwIfIllegalArgumentException(ret);
        if (ret.equals("null")) {
            return null;
        } else {
            return ret;
        }
    }

    protected String makeServletMethodCallWithExceptions(String methodName, String servletRequest) throws RegistryException, EntryNotFoundException {
        String ret = makeServletMethodCall(methodName, servletRequest);
        throwIfRegistryException(ret);
        throwIfEntryNotFoundException(ret);
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
    private List<String> convertToList(String methodName, String str) {
        /*
         * Something unlikely to occur in a DN, yet still readable. If this value changes
         * remember to update UserRegistryServlet#convertFromList() as well.
         */
        final String delimiter = " :: ";

        List<String> list = new ArrayList<String>();
        if (str.startsWith("[") && str.endsWith("]")) {
            if (str.length() == 2) {
                return list;
            } else {
                String[] contents = str.substring(1, str.length() - 1).split(delimiter);
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
    private SearchResult convertToSearchResult(String methodName, String str) {
        String[] tokens = str.split(" ");
        if ("SearchResult".equals(tokens[0])) {
            //            for (int i = 0; i < tokens.length; i++) {
            //                System.err.println(i + "=" + tokens[i]);
            //            }
            boolean hasMore = convertToBoolean("convertToSearchResult, token[1]", tokens[1].split("=")[1]);
            String listStr = str.substring(str.indexOf('['), str.indexOf(']') + 1);
            List<String> list = convertToList("convertToSearchResult, listStr", listStr);
            return new SearchResult(list, hasMore);
        } else {
            throw new IllegalStateException(methodName + " expected a SearchResult response, but received [" + str + "]");
        }
    }

    /**
     * Encode a string using {@link URLEncoder#encode(String, String)}
     *
     * @param string The string to encode.
     * @return The encoded string.
     */
    private String encodeForURI(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (Exception e) {
            throw new IllegalStateException("Error encoding string '" + string + "' for URI.", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getRealm() {
        String methodName = "getRealm";
        String servletRequest = "?method=" + methodName;
        return makeServletMethodCall(methodName, servletRequest);
    }

    /** {@inheritDoc} */
    @Override
    public String checkPassword(String userSecurityName, String password) throws RegistryException {
        String methodName = "checkPassword";
        String servletRequest = "?method=" + methodName +
                                "&userSecurityName=" + encodeForURI(userSecurityName)
                                + "&password=" + encodeForURI(password);
        return makeServletMethodCallWithException(methodName, servletRequest);
    }

    /** {@inheritDoc} */
    @Override
    public String mapCertificate(X509Certificate cert) throws CertificateMapNotSupportedException, CertificateMapFailedException, RegistryException {
        throw new CertificateMapNotSupportedException("UserRegistry servlet does not support mapCertificate right now.");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidUser(String userSecurityName) throws RegistryException {
        String methodName = "isValidUser";
        String servletRequest = "?method=" + methodName +
                                "&userSecurityName=" + encodeForURI(userSecurityName);
        String servletResponse = makeServletMethodCallWithException(methodName, servletRequest);
        return convertToBoolean(methodName, servletResponse);
    }

    /** {@inheritDoc} */
    @Override
    public SearchResult getUsers(String pattern, int limit) throws RegistryException {
        String methodName = "getUsers";
        String servletRequest = "?method=" + methodName +
                                "&pattern=" + encodeForURI(pattern) + "&limit=" + limit;
        String servletResponse = makeServletMethodCallWithException(methodName, servletRequest);

        return convertToSearchResult(methodName, servletResponse);
    }

    /** {@inheritDoc} */
    @Override
    public String getUserDisplayName(String userSecurityName) throws EntryNotFoundException, RegistryException {
        String methodName = "getUserDisplayName";
        String servletRequest = "?method=" + methodName +
                                "&userSecurityName=" + encodeForURI(userSecurityName);
        return makeServletMethodCallWithExceptions(methodName, servletRequest);
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueUserId(String userSecurityName) throws EntryNotFoundException, RegistryException {
        String methodName = "getUniqueUserId";
        String servletRequest = "?method=" + methodName +
                                "&userSecurityName=" + encodeForURI(userSecurityName);
        return makeServletMethodCallWithExceptions(methodName, servletRequest);
    }

    /** {@inheritDoc} */
    @Override
    public String getUserSecurityName(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        String methodName = "getUserSecurityName";
        String servletRequest = "?method=" + methodName +
                                "&uniqueUserId=" + encodeForURI(uniqueUserId);
        return makeServletMethodCallWithExceptions(methodName, servletRequest);
    }

    /** {@inheritDoc} */
    @Override
    public SearchResult getUsersForGroup(String groupSecurityName, int limit) throws EntryNotFoundException, RegistryException {
        String methodName = "getUsersForGroup";
        String servletRequest = "?method=" + methodName +
                                "&groupSecurityName=" + encodeForURI(groupSecurityName) + "&limit=" + limit;
        String servletResponse = makeServletMethodCallWithExceptions(methodName, servletRequest);

        return convertToSearchResult(methodName, servletResponse);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidGroup(String groupSecurityName) throws RegistryException {
        String methodName = "isValidGroup";
        String servletRequest = "?method=" + methodName +
                                "&groupSecurityName=" + encodeForURI(groupSecurityName);
        String servletResponse = makeServletMethodCallWithException(methodName, servletRequest);
        return convertToBoolean(methodName, servletResponse);
    }

    /** {@inheritDoc} */
    @Override
    public SearchResult getGroups(String pattern, int limit) throws RegistryException {
        String methodName = "getGroups";
        String servletRequest = "?method=" + methodName +
                                "&pattern=" + encodeForURI(pattern) + "&limit=" + limit;
        String servletResponse = makeServletMethodCallWithException(methodName, servletRequest);
        return convertToSearchResult(methodName, servletResponse);
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupDisplayName(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        String methodName = "getGroupDisplayName";
        String servletRequest = "?method=" + methodName +
                                "&groupSecurityName=" + encodeForURI(groupSecurityName);
        return makeServletMethodCallWithExceptions(methodName, servletRequest);
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueGroupId(String groupSecurityName) throws EntryNotFoundException, RegistryException {
        String methodName = "getUniqueGroupId";
        String servletRequest = "?method=" + methodName +
                                "&groupSecurityName=" + encodeForURI(groupSecurityName);
        return makeServletMethodCallWithExceptions(methodName, servletRequest);
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupSecurityName(String uniqueGroupId) throws EntryNotFoundException, RegistryException {
        String methodName = "getGroupSecurityName";
        String servletRequest = "?method=" + methodName +
                                "&uniqueGroupId=" + encodeForURI(uniqueGroupId);
        return makeServletMethodCallWithExceptions(methodName, servletRequest);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getUniqueGroupIdsForUser(String uniqueUserId) throws EntryNotFoundException, RegistryException {
        String methodName = "getUniqueGroupIdsForUser";
        String servletRequest = "?method=" + methodName +
                                "&uniqueUserId=" + encodeForURI(uniqueUserId);
        String servletResponse = makeServletMethodCallWithExceptions(methodName, servletRequest);
        return convertToList(methodName, servletResponse);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getGroupsForUser(String userSecurityName) throws EntryNotFoundException, RegistryException {
        String methodName = "getGroupsForUser";
        String servletRequest = "?method=" + methodName +
                                "&userSecurityName=" + encodeForURI(userSecurityName);
        String servletResponse = makeServletMethodCallWithExceptions(methodName, servletRequest);
        return convertToList(methodName, servletResponse);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.registry.UserRegistry#getType()
     */
    @Override
    public String getType() {
        String methodName = "getType";
        String servletRequest = "?method=" + methodName;
        return makeServletMethodCall(methodName, servletRequest);
    }

}
