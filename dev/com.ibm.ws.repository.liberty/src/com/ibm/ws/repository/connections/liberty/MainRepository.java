/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.connections.liberty;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;

import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.connections.RestRepositoryConnectionProxy;
import com.ibm.ws.repository.exceptions.RepositoryBackendIOException;
import com.ibm.ws.repository.exceptions.RepositoryHttpException;

/**
 *
 */
public class MainRepository {

    // Keys in the properties file
    private static final String REPOSITORY_URL_PROP = "repository.url";
    private static final String API_KEY_PROP = "apiKey";
    private static final String USERID_PROP = "userId";
    private static final String PASSWORD_PROP = "password";
    private static final String SOFTLAYER_USERID_PROP = "softlayerUserId";
    private static final String SOFTLAYER_PASSWORD_PROP = "softlayerPassword";
    private static final String ATTACHMENT_BASIC_AUTH_USERID_PROP = "attachmentBasicAuthUserId";
    private static final String ATTACHMENT_BASIC_AUTH_PASSWORD_PROP = "attachmentBasicAuthPassword";

    // System property to override the default properties file location
    public static final String LOCATION_OVERRIDE_SYS_PROP_NAME = "repository.description.url";

    // The default location for the properties file
    private static final String DEFAULT_PROPERTIES_FILE_LOCATION = "https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/downloads/assetservicelocation.props";

    private static volatile Properties repoProperties;

    /*
     * Private constructor so noone tries to creeate this static only class
     */
    private MainRepository() {

    }

    public static RestRepositoryConnection createConnection() throws RepositoryBackendIOException {
        return createConnection(null);
    }

    /**
     * Creates a LoginInfoEntry with a proxy. This will then load the default repository using a hosted properties file on DHE.
     *
     * @param proxy
     * @throws RepositoryBackendIOException
     */
    public static RestRepositoryConnection createConnection(RestRepositoryConnectionProxy proxy) throws RepositoryBackendIOException {
        readRepoProperties(proxy);

        RestRepositoryConnection connection = new RestRepositoryConnection(repoProperties.getProperty(REPOSITORY_URL_PROP).trim());
        connection.setProxy(proxy);

        if (repoProperties.containsKey(API_KEY_PROP)) {
            connection.setApiKey(repoProperties.getProperty(API_KEY_PROP).trim());
        }

        if (repoProperties.containsKey(USERID_PROP)) {
            connection.setUserId(repoProperties.getProperty(USERID_PROP).trim());
        }
        if (repoProperties.containsKey(PASSWORD_PROP)) {
            connection.setPassword(repoProperties.getProperty(PASSWORD_PROP).trim());
        }
        if (repoProperties.containsKey(SOFTLAYER_USERID_PROP)) {
            connection.setSoftlayerUserId(repoProperties.getProperty(SOFTLAYER_USERID_PROP).trim());
        }
        if (repoProperties.containsKey(SOFTLAYER_PASSWORD_PROP)) {
            connection.setSoftlayerPassword(repoProperties.getProperty(SOFTLAYER_PASSWORD_PROP).trim());
        }
        if (repoProperties.containsKey(ATTACHMENT_BASIC_AUTH_USERID_PROP)) {
            connection.setAttachmentBasicAuthUserId(repoProperties.getProperty(ATTACHMENT_BASIC_AUTH_USERID_PROP).trim());
        }
        if (repoProperties.containsKey(ATTACHMENT_BASIC_AUTH_PASSWORD_PROP)) {
            connection.setAttachmentBasicAuthPassword(repoProperties.getProperty(ATTACHMENT_BASIC_AUTH_PASSWORD_PROP).trim());
        }
        return connection;
    }

    /**
     * Clears the cached repository properties file so it will be re-read on the next create
     */
    public static void clearCachedRepoProperties() {
        repoProperties = null;
    }

    /**
     * Tests if the repository description properties file exists as defined by the
     * location override system property or at the default location
     *
     * @return true if the properties file exists, otherwise false
     */
    public static boolean repositoryDescriptionFileExists(RestRepositoryConnectionProxy proxy) {
        boolean exists = false;
        try {

            URL propertiesFileURL = getPropertiesFileLocation();

            // Are we accessing the properties file (from DHE) using a proxy ?
            if (proxy != null) {

                if (proxy.isHTTPorHTTPS()) {

                    Proxy javaNetProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getProxyURL().getHost(), proxy.getProxyURL().getPort()));
                    URLConnection connection = propertiesFileURL.openConnection(javaNetProxy);
                    InputStream is = connection.getInputStream();

                    exists = true;
                    is.close();

                    if (connection instanceof HttpURLConnection) {
                        ((HttpURLConnection) connection).disconnect();
                    }

                } else {
                    // The proxy is not an HTTP or HTTPS proxy we do not support this
                    UnsupportedOperationException ue = new UnsupportedOperationException("Non-HTTP proxy not supported");
                    throw new IOException(ue);
                }

            } else {
                // not using a proxy
                InputStream is = propertiesFileURL.openStream();
                exists = true;
                is.close();
            }

        } catch (MalformedURLException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }

        return exists;
    }

    private static void readRepoProperties(RestRepositoryConnectionProxy proxy) throws RepositoryBackendIOException {
        synchronized (MainRepository.class) {
            if (repoProperties == null) {
                URL propertiesFileURL = null;
                try {
                    propertiesFileURL = getPropertiesFileLocation();

                    Properties props = null;
                    if (proxy != null) {

                        if (proxy.isHTTPorHTTPS()) {

                            Proxy javaNetProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getProxyURL().getHost(), proxy.getProxyURL().getPort()));
                            URLConnection connection = propertiesFileURL.openConnection(javaNetProxy);

                            checkHttpResponseCodeValid(connection);

                            Reader reader = new InputStreamReader(connection.getInputStream(), "UTF-8");

                            props = new Properties();
                            try {
                                props.load(reader);
                            } finally {
                                reader.close();
                            }

                            if (connection instanceof HttpURLConnection) {
                                ((HttpURLConnection) connection).disconnect();
                            }

                        } else {
                            // The proxy is not an HTTP or HTTPS proxy we do not support this

                            UnsupportedOperationException ue = new UnsupportedOperationException("Non-HTTP proxy not supported");
                            throw new IOException(ue);
                        }

                    } else {
                        URLConnection connection = propertiesFileURL.openConnection();

                        checkHttpResponseCodeValid(connection);

                        Reader reader = new InputStreamReader(propertiesFileURL.openStream(), "UTF-8");
                        props = new Properties();
                        try {
                            props.load(reader);
                        } finally {
                            reader.close();
                        }
                    }

                    if (!!!props.containsKey(REPOSITORY_URL_PROP)) {
                        throw new IllegalArgumentException(REPOSITORY_URL_PROP);
                    }
                    if (!!!props.containsKey(API_KEY_PROP)) {
                        throw new IllegalArgumentException(API_KEY_PROP);
                    }

                    repoProperties = props;

                } catch (IOException e) {
                    throw new RepositoryBackendIOException("Failed to read properties file " + propertiesFileURL, e, null);
                }
            }
        }
    }

    private static URL getPropertiesFileLocation() throws MalformedURLException {
        final String overrideLocation = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(LOCATION_OVERRIDE_SYS_PROP_NAME);
            }
        });

        URL url;
        try {
            url = AccessController.doPrivileged(new PrivilegedExceptionAction<URL>() {
                @Override
                public URL run() throws MalformedURLException {
                    if (overrideLocation != null && overrideLocation.length() > 0) {
                        return new URL(overrideLocation);
                    } else {
                        return new URL(DEFAULT_PROPERTIES_FILE_LOCATION);
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            throw (MalformedURLException) e.getCause();
        }

        return url;
    }

    /**
     * Checks for a valid response code and throws and exception with the response code if an error
     *
     * @param connection
     * @throws RepositoryHttpException
     * @throws IOException
     */
    private static void checkHttpResponseCodeValid(URLConnection connection) throws RepositoryHttpException, IOException {
        // if HTTP URL not File URL
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection conn = (HttpURLConnection) connection;
            conn.setRequestMethod("GET");
            int respCode = conn.getResponseCode();
            if (respCode < 200 || respCode >= 300) {
                throw new RepositoryHttpException("HTTP connection returned error code " + respCode, respCode, null);
            }
        }
    }

}
