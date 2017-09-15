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
     * Gets the collection match for the web resource collection based on the following custom method algorithm.
     * <pre>
     * Custom method matching use case.
     * Happy path:
     * 1. Validate the resource name matches one of the URL patterns
     * 2. Validate the method matches
     * 3. Return the collection match found
     * 
     * Exceptional path:
     * 1.a If resource name does not match, return RESPONSE_NO_MATCH.
     * 2.a If method does not match, determine that it is listed and return RESPONSE_NO_MATCH.
     * 2.b When method is not listed, the match is null and it is processed by method getMatchResponse turning it into a CUSTOM_NO_MATCH_RESPONSE.
     * </pre>
     */
    @Override
    protected CollectionMatch getCollectionMatchForWebResourceCollection(WebResourceCollection webResourceCollection, String resourceName, String method) {
        CollectionMatch match = null;
        CollectionMatch collectionMatchFound = webResourceCollection.performUrlMatch(resourceName);
        if (collectionMatchFound != null) {
            if (webResourceCollection.isMethodMatched(method)) {
                match = collectionMatchFound;
            } else if (webResourceCollection.isMethodListed(method)) {
                match = CollectionMatch.RESPONSE_NO_MATCH;
            }
        } else {
            match = CollectionMatch.RESPONSE_NO_MATCH;
        }
        return match;
    }
}
