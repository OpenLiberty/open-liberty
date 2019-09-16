/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testing.opentracing.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.Future;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * <p>General purpose FAT service utilities.</p>
 */
public class FATUtilsService {
    // Request utils ...

    public static Future<Response> invokeAsync(String requestUrl) {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(requestUrl);
        return target.request().async().get();
    }

    public static Response invoke(String requestUrl) {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(requestUrl);
        return target.request().get();
    }

    // URL utilities ...

    public static String getRequestUrl(
        String hostName, int portNumber,
        String requestPath) {

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("http://");
        urlBuilder.append(hostName);
        urlBuilder.append(":");
        urlBuilder.append(Integer.toString(portNumber));
        urlBuilder.append("/");
        urlBuilder.append(requestPath);
        return urlBuilder.toString();
    }

    public static String getRequestPath(
        String contextRoot,
        String appPath, String servicePath, String endpointPath,
        Map<String, Object> requestParameters)
        throws UnsupportedEncodingException {

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(contextRoot);
        urlBuilder.append("/");
        urlBuilder.append(appPath);
        urlBuilder.append("/");
        urlBuilder.append(servicePath);
        urlBuilder.append("/");
        urlBuilder.append(endpointPath);

        if ( (requestParameters != null) && !requestParameters.isEmpty() ) {
            urlBuilder.append("?");

            String prefix = null;
            for ( Map.Entry<String, Object> parmEntry : requestParameters.entrySet() ) {
                String parmName = parmEntry.getKey();
                Object parmValue = parmEntry.getValue();

                if ( prefix != null ) {
                    urlBuilder.append(prefix);
                } else {
                    prefix = "&";
                }

                urlBuilder.append( urlEncode(parmName) );
                // 'encode' throws UnsupportedEncodingException

                if ( parmValue != null ) {
                    urlBuilder.append("=");
                    urlBuilder.append( urlEncode( parmValue.toString() ) );
                    // 'encode' throws UnsupportedEncodingException
                }
            }
        }

        return urlBuilder.toString();
    }

    public static final String UTF8_ENCODING = "UTF-8";

    public static String urlEncode(String text) throws UnsupportedEncodingException {
        return URLEncoder.encode(text, UTF8_ENCODING); // throws UnsupportedEncodingException
    }
}
