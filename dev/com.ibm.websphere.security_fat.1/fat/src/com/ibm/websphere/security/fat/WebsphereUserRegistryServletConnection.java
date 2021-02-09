/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.security.fat;

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
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.ibm.websphere.security.CertificateMapFailedException;
import com.ibm.websphere.security.CertificateMapNotSupportedException;
import com.ibm.websphere.security.CustomRegistryException;
import com.ibm.websphere.security.EntryNotFoundException;
import com.ibm.websphere.security.NotImplementedException;
import com.ibm.websphere.security.PasswordCheckFailedException;
import com.ibm.websphere.security.Result;
import com.ibm.websphere.security.UserRegistry;
import com.ibm.websphere.security.cred.WSCredential;

/**
 *
 */
public class WebsphereUserRegistryServletConnection implements UserRegistry {
    private static final Class<?> c = WebsphereUserRegistryServletConnection.class;
    private final Logger logger;
    private final String servletURL;
    private final String realmName;

    public WebsphereUserRegistryServletConnection(String host, int port, String realmName) {
        if (host == null || port == 0 || realmName == null)
            throw new IllegalArgumentException("Host (" + host + "is null or port (" + port + ") is zero or realmName is null");
        servletURL = "http://" + host + ":" + port + "/WebsphereUserRegistry";
        logger = Logger.getLogger(c.getCanonicalName());
        this.realmName = realmName;
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
                    URL url = new URL(servletURL + servletRequest + "&realmName=" + realmName);
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
     * @throws CustomRegistryException
     */
    private void throwIfCustomRegistryException(String ret) throws CustomRegistryException {
        if (!ret.startsWith(CustomRegistryException.class.getName()))
            return;
        String[] split = ret.split(": ", 2);
        String msg = split.length == 2 ? split[1] : split[0];
        throw new CustomRegistryException(msg);
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

    protected String makeServletMethodCallWithException(String methodName, String servletRequest) throws CustomRegistryException {
        String ret = makeServletMethodCall(methodName, servletRequest);
        throwIfCustomRegistryException(ret);
        if (ret.equals("null")) {
            return null;
        } else {
            return ret;
        }

    }

    protected String makeServletMethodCallWithExceptions(String methodName, String servletRequest) throws CustomRegistryException, EntryNotFoundException {
        String ret = makeServletMethodCall(methodName, servletRequest);
        throwIfCustomRegistryException(ret);
        throwIfEntryNotFoundException(ret);
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
    private Result convertToResult(String methodName, String str) {
        String[] tokens = str.split(" ");
        if ("Result".equals(tokens[0])) {
            //            for (int i = 0; i < tokens.length; i++) {
            //                System.err.println(i + "=" + tokens[i]);
            //            }
            boolean hasMore = convertToBoolean("convertToResult, token[1]", tokens[1].split("=")[1]);
            String listStr = str.substring(str.indexOf('['), str.indexOf(']') + 1);
            List<String> list = convertToList("convertToResult, listStr", listStr);
            Result result = new Result();
            result.setList(list);
            if (hasMore) {
                result.setHasMore();
            }
            return result;
        } else {
            throw new IllegalStateException(methodName + " expected a Result response, but recieved [" + str + "]");
        }
    }

    /**
     * @param userSecurityName
     * @return
     */
    private String encodeSpaces(String userSecurityName) {
        return userSecurityName.replaceAll(" ", "%20");
    }

    /** {@inheritDoc} */
    @Override
    public String getRealm() {
        String methodName = "getRealm";
        String servletRequest = "?method=" + methodName;
        return makeServletMethodCall(methodName, servletRequest);
    }

    /**
     * {@inheritDoc}
     *
     * @throws PasswordCheckFailedException
     */
    @Override
    public String checkPassword(String userSecurityName, String password) throws CustomRegistryException, PasswordCheckFailedException {
        String methodName = "checkPassword";
        String servletRequest = "?method=" + methodName +
                                "&userSecurityName=" + encodeSpaces(userSecurityName)
                                + "&password=" + encodeSpaces(password);
        String result = makeServletMethodCallWithException(methodName, servletRequest);
        if (result.contains("com.ibm.websphere.security.PasswordCheckFailedException")) {
            throw new PasswordCheckFailedException(result.split(" ")[1]);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String mapCertificate(X509Certificate[] cert) throws CertificateMapNotSupportedException, CertificateMapFailedException, CustomRegistryException {
        throw new CertificateMapNotSupportedException("UserRegistry servlet does not support mapCertificate right now.");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidUser(String userSecurityName) throws CustomRegistryException {
        String methodName = "isValidUser";
        String servletRequest = "?method=" + methodName +
                                "&userSecurityName=" + encodeSpaces(userSecurityName);
        String servletResponse = makeServletMethodCallWithException(methodName, servletRequest);
        return convertToBoolean(methodName, servletResponse);
    }

    /** {@inheritDoc} */
    @Override
    public Result getUsers(String pattern, int limit) throws CustomRegistryException {
        String methodName = "getUsers";
        String servletRequest = "?method=" + methodName +
                                "&pattern=" + encodeSpaces(pattern) + "&limit=" + limit;
        String servletResponse = makeServletMethodCallWithException(methodName, servletRequest);

        return convertToResult(methodName, servletResponse);
    }

    /** {@inheritDoc} */
    @Override
    public String getUserDisplayName(String userSecurityName) throws EntryNotFoundException, CustomRegistryException {
        String methodName = "getUserDisplayName";
        String servletRequest = "?method=" + methodName +
                                "&userSecurityName=" + encodeSpaces(userSecurityName);
        return makeServletMethodCallWithExceptions(methodName, servletRequest);
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueUserId(String userSecurityName) throws EntryNotFoundException, CustomRegistryException {
        String methodName = "getUniqueUserId";
        String servletRequest = "?method=" + methodName +
                                "&userSecurityName=" + encodeSpaces(userSecurityName);
        return makeServletMethodCallWithExceptions(methodName, servletRequest);
    }

    /** {@inheritDoc} */
    @Override
    public String getUserSecurityName(String uniqueUserId) throws EntryNotFoundException, CustomRegistryException {
        String methodName = "getUserSecurityName";
        String servletRequest = "?method=" + methodName +
                                "&uniqueUserId=" + encodeSpaces(uniqueUserId);
        return makeServletMethodCallWithExceptions(methodName, servletRequest);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValidGroup(String groupSecurityName) throws CustomRegistryException {
        String methodName = "isValidGroup";
        String servletRequest = "?method=" + methodName +
                                "&groupSecurityName=" + encodeSpaces(groupSecurityName);
        String servletResponse = makeServletMethodCallWithException(methodName, servletRequest);
        return convertToBoolean(methodName, servletResponse);
    }

    /** {@inheritDoc} */
    @Override
    public Result getGroups(String pattern, int limit) throws CustomRegistryException {
        String methodName = "getGroups";
        String servletRequest = "?method=" + methodName +
                                "&pattern=" + pattern + "&limit=" + limit;
        String servletResponse = makeServletMethodCallWithException(methodName, servletRequest);
        return convertToResult(methodName, servletResponse);
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupDisplayName(String groupSecurityName) throws EntryNotFoundException, CustomRegistryException {
        String methodName = "getGroupDisplayName";
        String servletRequest = "?method=" + methodName +
                                "&groupSecurityName=" + encodeSpaces(groupSecurityName);
        return makeServletMethodCallWithExceptions(methodName, servletRequest);
    }

    /** {@inheritDoc} */
    @Override
    public String getUniqueGroupId(String groupSecurityName) throws EntryNotFoundException, CustomRegistryException {
        String methodName = "getUniqueGroupId";
        String servletRequest = "?method=" + methodName +
                                "&groupSecurityName=" + encodeSpaces(groupSecurityName);
        return makeServletMethodCallWithExceptions(methodName, servletRequest);
    }

    /** {@inheritDoc} */
    @Override
    public String getGroupSecurityName(String uniqueGroupId) throws EntryNotFoundException, CustomRegistryException {
        String methodName = "getGroupSecurityName";
        String servletRequest = "?method=" + methodName +
                                "&uniqueGroupId=" + encodeSpaces(uniqueGroupId);
        return makeServletMethodCallWithExceptions(methodName, servletRequest);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getUniqueGroupIds(String uniqueUserId) throws EntryNotFoundException, CustomRegistryException {
        String methodName = "getUniqueGroupIdsForUser";
        String servletRequest = "?method=" + methodName +
                                "&uniqueUserId=" + encodeSpaces(uniqueUserId);
        String servletResponse = makeServletMethodCallWithExceptions(methodName, servletRequest);
        return convertToList(methodName, servletResponse);
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getGroupsForUser(String userSecurityName) throws EntryNotFoundException, CustomRegistryException {
        String methodName = "getGroupsForUser";
        String servletRequest = "?method=" + methodName +
                                "&userSecurityName=" + encodeSpaces(userSecurityName);
        String servletResponse = makeServletMethodCallWithExceptions(methodName, servletRequest);
        return convertToList(methodName, servletResponse);
    }

    /** Not used */
    @Override
    public WSCredential createCredential(String userSecurityName) throws NotImplementedException, EntryNotFoundException, CustomRegistryException, RemoteException {

        return null;
    }

    /** Depecrated */
    @Override
    public Result getUsersForGroup(String groupSecurityName, int limit) throws NotImplementedException, EntryNotFoundException, CustomRegistryException, RemoteException {

        return null;
    }

    /** Not Used */
    @Override
    public void initialize(Properties props) throws CustomRegistryException, RemoteException {

    }

}
