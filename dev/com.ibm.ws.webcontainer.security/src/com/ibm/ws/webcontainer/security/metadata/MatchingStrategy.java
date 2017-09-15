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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The matching strategy is used to determine what constraints match the given resource access,
 * where the constraints are represented by the MatchResponse object and it contains the roles,
 * if SSL is required, and if access is precluded for such access. The concrete strategy is based
 * on the type of HTTP method. A resource access with a standard HTTP method will use the
 * StandardMatchingStrategy while a resource access with a custom HTTP method will use the
 * CustomMatchingStrategy.
 */
public abstract class MatchingStrategy {

    private static final Set<String> STANDARD_METHODS = new HashSet<String>(Arrays.asList("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE"));
    private static StandardMatchingStrategy standardMatchingStrategy = null;
    private static CustomMatchingStrategy customMatchingStrategy = null;

    /**
     * Determine what constraints match the given resource access.
     * 
     * @param securityConstraintCollection the security constraints to iterate through.
     * @param resourceName the resource name.
     * @param method the HTTP method.
     * @return the match response.
     */
    public static MatchResponse match(SecurityConstraintCollection securityConstraintCollection, String resourceName, String method) {
        MatchingStrategy matchingStrategy = getMatchingStrategy(method);
        return matchingStrategy.performMatch(securityConstraintCollection, resourceName, method);
    }

    private static MatchingStrategy getMatchingStrategy(String method) {
        MatchingStrategy matchingStrategy;
        if (isStandardHttpMethod(method)) {
            matchingStrategy = getStandardMatchingStrategy();
        } else {
            matchingStrategy = getCustomMatchingStrategy();
        }
        return matchingStrategy;
    }

    /**
     * An HTTP method is standard if it is one of the seven HTTP methods.
     * GET, POST, DELETE, PUT, HEAD, OPTIONS, and TRACE.
     * 
     * @param method The HTTP method to determine if it is standard.
     * @return The boolean value indicating if the method is standard or not.
     */
    public static boolean isStandardHttpMethod(String method) {
        return STANDARD_METHODS.contains(method);
    }

    private static MatchingStrategy getCustomMatchingStrategy() {
        if (customMatchingStrategy != null) {
            return customMatchingStrategy;
        }
        customMatchingStrategy = new CustomMatchingStrategy();
        return customMatchingStrategy;
    }

    private static MatchingStrategy getStandardMatchingStrategy() {
        if (standardMatchingStrategy != null) {
            return standardMatchingStrategy;
        }
        standardMatchingStrategy = new StandardMatchingStrategy();
        return standardMatchingStrategy;
    }

    /**
     * Common iteration to get the best constraint match response from the security constraints.
     * <pre>
     * To get the aggregated match response,
     * 
     * 1. For each security constraint,
     * 1a. Ask security constraint for its match response for the resource access.
     * 1b. Optionally set the default match response of the aggregate after receiving a response for a security constraint.
     * 1c. Aggregate the responses. See {@link com.ibm.ws.webcontainer.security.metadata.MatchResponse#aggregateResponse(ResponseAggregate)}.
     * 2. Finally, select from the aggregate the response.
     * </pre>
     * 
     * @param securityConstraintCollection the security constraints to iterate through.
     * @param resourceName the resource name.
     * @param method the HTTP method.
     * @return the match response.
     */
    public MatchResponse performMatch(SecurityConstraintCollection securityConstraintCollection, String resourceName, String method) {
        MatchResponse currentResponse = null;
        ResponseAggregate responseAggregate = createResponseAggregate();

        for (SecurityConstraint securityConstraint : securityConstraintCollection.getSecurityConstraints()) {
            currentResponse = getMatchResponse(securityConstraint, resourceName, method);

            optionallySetAggregateResponseDefault(currentResponse, responseAggregate);
            if (isMatch(currentResponse)) {
                responseAggregate = currentResponse.aggregateResponse(responseAggregate);
            }
        }

        return responseAggregate.selectMatchResponse();
    }

    /**
     * The concrete strategy must provide the response aggregate to hold the response state.
     * 
     * @return the response aggregate.
     */
    protected abstract ResponseAggregate createResponseAggregate();

    /**
     * The concrete strategy must implement the algorithm to obtain the match response.
     * 
     * @param securityConstraint the security constraint used to determine match.
     * @param resourceName the resource name.
     * @param method the HTTP method.
     * @return the match response for the specified security constraint.
     */
    protected abstract MatchResponse getMatchResponse(SecurityConstraint securityConstraint, String resourceName, String method);

    /**
     * The concrete matching strategy may choose to change the default match response of
     * the aggregate after receiving a response for a security constraint.
     * 
     * @param currentResponse the current response received for the security constraint.
     * @param responseAggregate the response aggregate.
     */
    protected void optionallySetAggregateResponseDefault(MatchResponse currentResponse, ResponseAggregate responseAggregate) {
        // This is a no-op method that can be overridden by the concrete sub-classes.
    }

    /**
     * Determines if the current response is a match that can be aggregated.
     * The concrete strategy must determine if the response is a match.
     * This method is used by the <code>performMatch</code> method in order to
     * determine if the response needs to be aggregated to the previous responses.
     * There is no need to aggregate no matches.
     * 
     * @param currentResponse
     * @return <code>true</code> if the current response is a match.
     */
    protected abstract boolean isMatch(MatchResponse currentResponse);

    /**
     * Gets the match for the resource access. The web resource collections are
     * individually used to determine what is the match for this resource access. The best match is selected.
     * The seven standard HTTP methods are,
     * GET, POST, DELETE, PUT, HEAD, OPTIONS, and TRACE.
     * 
     * @param resourceName The resource name.
     * @param method The HTTP method.
     * @return the collection match.
     */
    protected CollectionMatch getCollectionMatch(List<WebResourceCollection> webResourceCollections, String resourceName, String method) {
        CollectionMatch bestMatch = getInitialCollectionMatch();
        for (WebResourceCollection webResourceCollection : webResourceCollections) {

            CollectionMatch currentMatch = getCollectionMatchForWebResourceCollection(webResourceCollection, resourceName, method);
            if (currentMatch != null) {
                bestMatch = selectBestCollectionMatch(bestMatch, currentMatch);
            }

            if (bestMatch != null && bestMatch.isExactMatch()) {
                break;
            }
        }

        return bestMatch;
    }

    /**
     * The concrete strategy must provide the initial collection match to be used.
     * 
     * @return the initial collection match.
     */
    protected abstract CollectionMatch getInitialCollectionMatch();

    /**
     * The concrete strategy must implement the algorithm to obtain the collection match from the web resource collection.
     * 
     * @param webResourceCollection the web resource collection to iterate through.
     * @param resourceName the resource name.
     * @param method the HTTP method.
     * @return the collection match.
     */
    protected abstract CollectionMatch getCollectionMatchForWebResourceCollection(WebResourceCollection webResourceCollection, String resourceName, String method);

    /**
     * Exact match is best,
     * then path match with longest URI matched,
     * finally extension match.
     */
    protected CollectionMatch selectBestCollectionMatch(CollectionMatch previousMatch, CollectionMatch currentMatch) {

        CollectionMatch bestMatch = null;
        if (previousMatch == null) {
            bestMatch = currentMatch;
        } else if (previousMatch.isDenyMatchByOmission()) {
            bestMatch = previousMatch;
        } else if (currentMatch.isDenyMatchByOmission()) {
            bestMatch = currentMatch;
        } else if (previousMatch.isDenyMatch()) {
            bestMatch = previousMatch;
        } else if (currentMatch.isDenyMatch()) {
            bestMatch = currentMatch;
        } else if (previousMatch.isPermitMatch()) {
            bestMatch = previousMatch;
        } else if (currentMatch.isPermitMatch()) {
            bestMatch = currentMatch;
        } else if (previousMatch.isExactMatch()) {
            bestMatch = previousMatch;
        } else if (currentMatch.isExactMatch()) {
            bestMatch = currentMatch;
        } else if (previousMatch.isPathMatch() && previousMatch.getUrlPattern().length() >= currentMatch.getUrlPattern().length()) {
            bestMatch = previousMatch;
        } else {
            bestMatch = currentMatch;
        }
        return bestMatch;
    }

}
