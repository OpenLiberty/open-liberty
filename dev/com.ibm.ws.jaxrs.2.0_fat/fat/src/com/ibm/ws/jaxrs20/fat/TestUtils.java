/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;

/**
 * JAX-RS Fat utility class for some commonly used functionality.
 * More methods to be added later.
 */
public class TestUtils {

    public static int getPort() {
        return Integer.valueOf(System.getProperty("HTTP_default", "8000"));
    }

    /**
     * Utility method for converting HttpResponse to String
     *
     * @param HttpResponse response
     * @return String representation of response
     * @throws IOException
     */
    public static String asString(HttpResponse response) throws IOException {
        if (response.getEntity() == null) {
            return "";
        }

        final InputStream in = response.getEntity().getContent();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
            out.flush();
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    // TODO: In the getBaseTestUri() methods, need to make sure we
    // are not appending double forward slashes

    /**
     * Utility for constructing the resource URL needed for
     * test client to call service.
     *
     * @param contextRoot Tests assume this is the WAR file name
     * @param urlPattern Specified in web.xml's url-pattern
     * @param path Value of resource's @Path annotation
     * @return
     */
    public static String getBaseTestUri(String contextRoot, String urlPattern, String resourcePath) {

        // If any of the parameters are null, return empty; usage error
        if (contextRoot == null || urlPattern == null || resourcePath == null) {
            System.out.println("getBaseTestUri(contextRoot, urlPattern, resourcePath) returning empty string");
            return "";
        }

        // For tests, service will always be in the same machine
        StringBuffer sb = new StringBuffer("http://localhost:");
        sb.append(getPort());
        sb.append("/");
        sb.append(contextRoot);
        sb.append("/");
        sb.append(urlPattern);
        sb.append("/");
        sb.append(resourcePath);
        return sb.toString();
    }

    /**
     * Utility for constructing the resource URL needed for
     * test client to call service.
     *
     * @param contextRoot Tests assume this is the WAR file name
     * @param path Either the url-pattern or the resource path
     * @return
     */
    public static String getBaseTestUri(String contextRoot, String path) {

        // If either of the parameters are null, return empty; usage error
        if (contextRoot == null || path == null) {
            System.out.println("getBaseTestUri(contextRoot, path) returning empty string");
            return "";
        }

        // For tests, service will always be in the same machine
        StringBuffer sb = new StringBuffer("http://localhost:");
        sb.append(getPort());
        sb.append("/");
        sb.append(contextRoot);
        sb.append("/");
        sb.append(path);
        return sb.toString();
    }

    /**
     * Utility for constructing the resource URL needed for
     * test client to call service.
     *
     * @param contextRoot Tests assume this is the WAR file name
     * @param path Either the url-pattern or the resource path
     * @return
     */
    public static String getBaseTestUri(String contextRoot, String urlPattern, String resourcePath, String query) {

        // If any of the parameters are null, return empty; usage error
        if (contextRoot == null || urlPattern == null || resourcePath == null || query == null) {
            System.out.println("getBaseTestUri(contextRoot, urlPattern, resourcePath, query) returning empty string");
            return "";
        }

        // For tests, service will always be in the same machine
        StringBuffer sb = new StringBuffer("http://localhost:");
        sb.append(getPort());
        sb.append("/");
        sb.append(contextRoot);
        sb.append("/");
        sb.append(urlPattern);
        sb.append("/");
        sb.append(resourcePath);
        sb.append("/");
        sb.append(query);
        return sb.toString();
    }

    public static <T> T readEntity(Class<T> clazz, Response response) {
        T entity;
        // ugly but needed to avoid java.lang.NoSuchMethodError
        if (response instanceof org.apache.cxf.jaxrs.impl.ResponseImpl) {
            entity = ((org.apache.cxf.jaxrs.impl.ResponseImpl) response).readEntity(clazz);
        } else {
            entity = response.readEntity(clazz);
        }
        return entity;
    }

    public static String getHeaderString(String header, Response response) {
        String value;
        // ugly but needed to avoid java.lang.NoSuchMethodError
        if (response instanceof org.apache.cxf.jaxrs.impl.ResponseImpl) {
            value = ((org.apache.cxf.jaxrs.impl.ResponseImpl) response).getHeaderString(header);
        } else {
            value = response.getHeaderString(header);
        }
        return value;
    }

    public static MediaType getMediaType(Response response) {
        MediaType mediaType;
        // ugly but needed to avoid java.lang.NoSuchMethodError
        if (response instanceof org.apache.cxf.jaxrs.impl.ResponseImpl) {
            mediaType = ((org.apache.cxf.jaxrs.impl.ResponseImpl) response).getMediaType();
        } else {
            mediaType = response.getMediaType();
        }
        return mediaType;
    }
}
