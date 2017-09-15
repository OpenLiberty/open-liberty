/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.container.osgi.internal.url;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerService;

import com.ibm.ws.artifact.url.WSJarURLConnection;

@Component(service = URLStreamHandlerService.class,
           property = "url.handler.protocol=" + WSJPAUrlUtils.WSJPA_PROTOCOL_NAME)
public class JPAURLStreamHandler extends AbstractURLStreamHandlerService {
    @Override
    public URLConnection openConnection(URL url) throws IOException {
        URL embeddedURL = null;
        URLConnection urlConnection = null;

        if (url == null) {
            throw new IOException("JPAURLStreamHandler.openConnection cannot take a null URL argument.");
        }

        // Extract the URL embedded in the wsjpa URL's path            
        try {
            embeddedURL = WSJPAUrlUtils.extractEmbeddedURL(url);

            // Check that the protocol for the embedded URL is wsjar, nothing else is currently supported
            String embeddedURLProtocol = embeddedURL.getProtocol();
            if (embeddedURLProtocol != null && !embeddedURLProtocol.equalsIgnoreCase("wsjar")) {
                // Unsupported embedded URL Protocol.
                throw new IOException("The \"" + WSJPAUrlUtils.WSJPA_PROTOCOL_NAME +
                                      "\" URL protocol cannot accept embedded URL with protocol \"" +
                                      embeddedURLProtocol + "\".");
            }
        } catch (MalformedURLException mue) {
            // Embedded URL is malformed.
            throw mue;
        }

        // Open the connection.  Wrap in a JPAWSJarURLConnection if the open operation returns a WSJarURLConnection.
        urlConnection = embeddedURL.openConnection();
        if (urlConnection instanceof WSJarURLConnection) {
            urlConnection = new JPAWSJarURLConnection(url, (WSJarURLConnection) urlConnection);
        }

        return urlConnection;
    }

    /**
     * Compare two urls to see whether they refer to the same file, i.e., having the same protocol, host, port,
     * and path. This method requires that none of its arguments is null. This is guaranteed by the fact that it
     * is only called indirectly by java.net.URL class.
     * 
     */
    @Override
    public boolean equals(URL u1, URL u2) {
        // Consult superclass version of equals(), first.
        if (super.equals(u1, u2)) {
            return true;
        }

        return checkEquality(u1, u2);
    }

    @Override
    public boolean sameFile(URL u1, URL u2) {
        // Consult superclass version of sameFile(), first.
        if (super.sameFile(u1, u2)) {
            return true;
        }

        return checkEquality(u1, u2);
    }

    private boolean checkEquality(URL u1, URL u2) {
        // The superclass version of equals() doesn't recognize that this is an encapsulating URL type, so
        // "jpa:wsjar:thing.jar!/" should be equal to "wsjar:thing.jar!/".
        String u1Protocol = u1.getProtocol();
        String u2Protocol = u2.getProtocol();

        // First test if both URL arguments are wsjpa URLs.  If so, then the superclass equals() method results are
        // sufficient.
        if (WSJPAUrlUtils.WSJPA_PROTOCOL_NAME.equalsIgnoreCase(u1Protocol) &&
            WSJPAUrlUtils.WSJPA_PROTOCOL_NAME.equalsIgnoreCase(u2Protocol)) {

            return false;
        }

        // Determine which URL is the jpa URL
        URL jpaURL = null;
        URL nonJpaURL = null;

        if (u1Protocol.equalsIgnoreCase(WSJPAUrlUtils.WSJPA_PROTOCOL_NAME)) {
            // u1 is the jpa URL
            jpaURL = u1;
            nonJpaURL = u2;
        } else {
            // u2 is the jpa URL
            jpaURL = u2;
            nonJpaURL = u1;
        }

        // Check if the non-jpa URL is a protocol that is currently supported for encapsulation by the wsjpa URL protocol
        if (!"wsjar".equalsIgnoreCase(nonJpaURL.getProtocol())) {
            return false;
        }

        // Generate a URL based on the path of the wsjpa URL, and compare the two.
        boolean returnValue = false;
        URI uri1, uri2 = null;
        try {
            URL jpaEncapsulatedPath = WSJPAUrlUtils.extractEmbeddedURL(jpaURL);
            uri1 = jpaEncapsulatedPath.toURI();
            uri2 = nonJpaURL.toURI();

            returnValue = uri1.equals(uri2);
        } catch (MalformedURLException e) {
            e.getClass(); // findbugs
            // A Malformed URL cannot ever be equal to a Non-Malformed URL.
            return false;
        } catch (URISyntaxException e) {
            e.getClass(); // findbugs
            return false;
        }

        return returnValue;
    }
}
