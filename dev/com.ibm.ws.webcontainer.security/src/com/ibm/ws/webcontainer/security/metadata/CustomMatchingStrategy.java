/*******************************************************************************
 * Copyright (c) 2012-2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.metadata;

/**
 * The strategy for matching resource accesses that use custom HTTP methods.
 * If the method is listed in either http-method or http-method-omission,
 * then treat the method as any standard method,
 * otherwise return CUSTOM_NO_MATCH_RESPONSE so that the collaborator is
 * able to return a 403 reply.
 */
public class CustomMatchingStrategy extends MatchingStrategy {

    @Override
    protected ResponseAggregate createResponseAggregate() {
        return new ResponseAggregate(MatchResponse.NO_MATCH_RESPONSE);
    }

    @Override
    protected void optionallySetAggregateResponseDefault(MatchResponse currentResponse, ResponseAggregate responseAggregate) {
        if (MatchResponse.CUSTOM_NO_MATCH_RESPONSE.equals(currentResponse)) {
            responseAggregate.setDefaultResponse(MatchResponse.CUSTOM_NO_MATCH_RESPONSE);
        }
    }

    @Override
    protected boolean isMatch(MatchResponse currentResponse) {
        return MatchResponse.CUSTOM_NO_MATCH_RESPONSE.equals(currentResponse) == false &&
                        MatchResponse.NO_MATCH_RESPONSE.equals(currentResponse) == false;
    }

    /**
     * Gets the response object that contains the roles, the SSL required
     * and access precluded indicators. Gets the response using the custom method algorithm.
     * If the collection match returned from the collection is null,
     * then response must be CUSTOM_NO_MATCH_RESPONSE.
     * 
     * @param resourceName The resource name.
     * @param method The HTTP method.
     * @return
     */
    @Override
    public MatchResponse getMatchResponse(SecurityConstraint securityConstraint, String resourceName, String method) {
        CollectionMatch collectionMatch = getCollectionMatch(securityConstraint.getWebResourceCollections(), resourceName, method);
        if (CollectionMatch.RESPONSE_NO_MATCH.equals(collectionMatch) ||
                        (collectionMatch == null && securityConstraint.getRoles().isEmpty() && securityConstraint.isAccessPrecluded() == false)) {
            return MatchResponse.NO_MATCH_RESPONSE;
        } else if (collectionMatch == null) {
            return MatchResponse.CUSTOM_NO_MATCH_RESPONSE;
        }
        return new MatchResponse(securityConstraint.getRoles(), securityConstraint.isSSLRequired(),
                                 securityConstraint.isAccessPrecluded(), collectionMatch);
    }

    @Override
    protected CollectionMatch getInitialCollectionMatch() {
        return null;
    }

    /**
     * getCollectionMatchForWebResourceCollection method in CustomMatchingStrategy is the same as StandardMatchingStrategy 
     * Both denyUncovered and omissionMethod keywords are applicable as well. 
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
