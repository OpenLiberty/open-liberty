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
package com.ibm.ws.webcontainer.security.metadata;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.webcontainer.security.metadata.SecurityConstraint;
import com.ibm.ws.webcontainer.security.metadata.WebResourceCollection;

/**
 * This test helper class builds a list of SecurityConstraint objects.
 * The usage pattern is to optionally perform a series of calls to buildWebResourceCollection(...)
 * and when all the WebResourceCollection objects for the desired SecurityContraint then call
 * buildSecurityConstraint(...). Repeat until all SecurityConstraint objects are built. Finally,
 * call getSecurityConstraints() to get the list of SecurityContraint objects.
 */
public class SecurityConstraintsBuilder {

    List<WebResourceCollection> webResourceCollections;
    List<SecurityConstraint> securityConstraints;

    public SecurityConstraintsBuilder() {
        webResourceCollections = new ArrayList<WebResourceCollection>();
        securityConstraints = new ArrayList<SecurityConstraint>();
    }

    public void buildWebResourceCollection(String[] methodArray, String... urlArray) {
        List<String> urlPatterns = createListFromArray(urlArray);
        List<String> methods = createListFromArray(methodArray);
        webResourceCollections.add(new WebResourceCollection(urlPatterns, methods));
    }

    public void buildWebResourceCollection(String[] methodArray, String[] omissionMethodArray, String... urlArray) {
        List<String> urlPatterns = createListFromArray(urlArray);
        List<String> methods = createListFromArray(methodArray);
        List<String> omissionMethods = createListFromArray(omissionMethodArray);
        webResourceCollections.add(new WebResourceCollection(urlPatterns, methods, omissionMethods));
    }

    public void buildSecurityConstraint(List<String> roles, boolean sslRequired, boolean accessPrecluded, boolean fromHttpConstraint, boolean accessUncovered) {
        List<WebResourceCollection> collections = new ArrayList<WebResourceCollection>(webResourceCollections);
        SecurityConstraint securityConstraint = new SecurityConstraint(collections, roles, sslRequired, accessPrecluded, fromHttpConstraint, accessUncovered);
        securityConstraints.add(securityConstraint);
        clearWebResourceCollectionsForNextConstraint();
    }

    public List<SecurityConstraint> getSecurityConstraints() {
        return securityConstraints;
    }

    private List<String> createListFromArray(String[] array) {
        List<String> list = new ArrayList<String>();
        for (String value : array) {
            list.add(value);
        }
        return list;
    }

    private void clearWebResourceCollectionsForNextConstraint() {
        webResourceCollections.clear();
    }
}
