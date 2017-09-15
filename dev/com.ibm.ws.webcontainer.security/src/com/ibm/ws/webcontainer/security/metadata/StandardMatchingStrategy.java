/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.metadata;

/**
 * The strategy for matching resource accesses that use standard HTTP methods.
 * For a standard HTTP method resource access iterate through each web resource
 * collection in each security constraint to determine what collections match.
 * The responses are aggregated in order to merge roles if necessary and then the
 * best response is selected.
 * See {@link com.ibm.ws.webcontainer.security.metadata.MatchingStrategy}, {@link com.ibm.ws.webcontainer.security.metadata.ResponseAggregate}, and
 * {@link com.ibm.ws.webcontainer.security.internal.metadata.MatchingResponse}.
 */
public class StandardMatchingStrategy extends MatchingStrategy {

    @Override
    protected ResponseAggregate createResponseAggregate() {
        return new ResponseAggregate(MatchResponse.NO_MATCH_RESPONSE);
    }

    @Override
    protected boolean isMatch(MatchResponse currentResponse) {

        if (MatchResponse.NO_MATCH_RESPONSE.equals(currentResponse)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Gets the response object that contains the roles, the SSL required
     * and access precluded indicators. Gets the response using the standard method algorithm.
     * 
     * @param resourceName The resource name.
     * @param method The HTTP method.
     * @return
     */
    @Override
    protected MatchResponse getMatchResponse(SecurityConstraint securityConstraint, String resourceName, String method) {
        CollectionMatch collectionMatch = getCollectionMatch(securityConstraint.getWebResourceCollections(), resourceName, method);

        if (CollectionMatch.RESPONSE_NO_MATCH.equals(collectionMatch)) {
            return MatchResponse.NO_MATCH_RESPONSE;
        }

        if (com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= 31) {
            if (collectionMatch.isExactMatch() && securityConstraint.isAccessUncovered() && securityConstraint.isFromHttpConstraint()) {
                return new MatchResponse(securityConstraint.getRoles(), securityConstraint.isSSLRequired(),
                                securityConstraint.isAccessPrecluded(), CollectionMatch.RESPONSE_PERMIT);
            }
        }

        return new MatchResponse(securityConstraint.getRoles(), securityConstraint.isSSLRequired(),
                        securityConstraint.isAccessPrecluded(), collectionMatch);
    }

    @Override
    protected CollectionMatch getInitialCollectionMatch() {
        return CollectionMatch.RESPONSE_NO_MATCH;
    }

    /**
     * To get a match with a standard HTTP method,
     * <pre>
     * 1. Determine if the method matches
     * 2. Perform a URL match
     * </pre>
     */
    @Override
    protected CollectionMatch getCollectionMatchForWebResourceCollection(WebResourceCollection webResourceCollection, String resourceName, String method) {
        CollectionMatch match = null;
        if (webResourceCollection.isMethodMatched(method)) {
            match = webResourceCollection.performUrlMatch(resourceName);
            if (match == null) {
                match = CollectionMatch.RESPONSE_NO_MATCH;
            }

        } else if (webResourceCollection.deniedDueToDenyUncoveredHttpMethods(method)) {

            if (webResourceCollection.isSpecifiedOmissionMethod(method)) {
                match = webResourceCollection.performUrlMatch(resourceName);
                if (match != null && !CollectionMatch.RESPONSE_NO_MATCH.equals(match) && !CollectionMatch.RESPONSE_DENY_MATCH.equals(match)) {
                    // meaning we have a match, so the url matches but the method is uncovered.  We return response deny by omission
                    match = CollectionMatch.RESPONSE_DENY_MATCH_BY_OMISSION;
                }

            } else {
                match = webResourceCollection.performUrlMatch(resourceName);
                if (match != null && !CollectionMatch.RESPONSE_NO_MATCH.equals(match) && !CollectionMatch.RESPONSE_DENY_MATCH.equals(match)) {
                    // meaning we have a match, so the url matches but the method is uncovered.  We return response deny
                    match = CollectionMatch.RESPONSE_DENY_MATCH;
                }

            }
        }
        return match;
    }

}
