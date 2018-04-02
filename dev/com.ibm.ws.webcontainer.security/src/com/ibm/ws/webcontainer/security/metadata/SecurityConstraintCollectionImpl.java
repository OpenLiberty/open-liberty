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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a collection of security constraint objects.
 */
public class SecurityConstraintCollectionImpl implements SecurityConstraintCollection {

    private final List<SecurityConstraint> securityConstraints;
    private static final Set<String> STANDARD_METHODS = new HashSet<String>(Arrays.asList("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE"));

    public SecurityConstraintCollectionImpl(List<SecurityConstraint> securityConstraints) {
        this.securityConstraints = securityConstraints;
    }

    /** {@inheritDoc} */
    @Override
    public MatchResponse getMatchResponse(String resourceName, String method) {
        if (securityConstraints == null || securityConstraints.isEmpty()) {
            return MatchResponse.NO_MATCH_RESPONSE;
        }

        return MatchingStrategy.match(this, resourceName, method);
    }

    @Override
    public List<SecurityConstraint> getSecurityConstraints() {
        return securityConstraints;
    }

    @Override
    public void addSecurityConstraints(List<SecurityConstraint> securityConstraints) {
        this.securityConstraints.addAll(securityConstraints);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection#getMatchResponse(java.lang.String, java.lang.String[])
     */
    @Override
    public List<MatchResponse> getMatchResponses(String resourceName, String... methods) {
        List<MatchResponse> matchResponses = new ArrayList<MatchResponse>();
        Set<String> methodList = new HashSet<String>();
        if (securityConstraints == null || securityConstraints.isEmpty()) {
            return null;
        }

        if (methods == null || (methods.length == 0) || (methods.length == 1 && methods[0].isEmpty())) {
            // fill in code to get list of all methods here start with standard list then add in custom methods
            methodList.addAll(STANDARD_METHODS);

            //got through the securityConstraints to find methods
            for (SecurityConstraint securityConstraint : securityConstraints) {
                List<WebResourceCollection> webCollections = securityConstraint.getWebResourceCollections();
                for (WebResourceCollection webResource : webCollections) {
                    if (webResource.performUrlMatch(resourceName) != null) {
                        List<String> httpMethodsOnResouce = webResource.getHttpMethods();
                        methodList.addAll(httpMethodsOnResouce);
                    }
                }
            }
        } else {
            methodList.addAll(new HashSet<String>(Arrays.asList(methods)));
        }

        for (String method : methodList) {
            MatchResponse match = getMatchResponse(resourceName, method);
            matchResponses.add(match);
        }
        return matchResponses;
    }

}
