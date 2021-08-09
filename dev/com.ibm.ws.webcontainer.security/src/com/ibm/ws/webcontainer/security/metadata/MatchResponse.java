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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The MatchResponse contains the roles. It can be used to determine if SSL is required
 * and if access is precluded.
 */
public class MatchResponse {

    @SuppressWarnings("unchecked")
    public static final MatchResponse NO_MATCH_RESPONSE = new MatchResponse(Collections.EMPTY_LIST, false, false);
    @SuppressWarnings("unchecked")
    public static final MatchResponse CUSTOM_NO_MATCH_RESPONSE = new MatchResponse(Collections.EMPTY_LIST, false, true);
    @SuppressWarnings("unchecked")
    public static final MatchResponse DENY_MATCH_RESPONSE = new MatchResponse(Collections.EMPTY_LIST, false, false);
    @SuppressWarnings("unchecked")
    public static final MatchResponse DENY_MATCH_BY_OMISSION_RESPONSE = new MatchResponse(Collections.EMPTY_LIST, false, false);

    private final List<String> roles;
    private final boolean sslRequired;
    private final boolean accessPrecluded;
    private CollectionMatch collectionMatch;

    /**
     * Constructs a MatchResponse object. List parameters are guaranteed to contain values or be empty lists.
     * 
     * @param roles The list of roles. Cannot be <code>null</code>.
     * @param sslRequired The flag that indicates if SSL is required.
     * @param accessPrecluded The flag that indicates if access is precluded.
     */
    public MatchResponse(List<String> roles, boolean sslRequired, boolean accessPrecluded) {
        if (accessPrecluded == true && roles != null && roles.size() > 0) {
            throw new IllegalArgumentException("The roles must be empty when access is precluded.");
        }
        this.roles = roles;
        this.sslRequired = sslRequired;
        this.accessPrecluded = accessPrecluded;
    }

    /**
     * Constructs a MatchResponse object. List parameters are guaranteed to contain values or be empty lists.
     * 
     * @param roles The list of roles. Cannot be <code>null</code>.
     * @param sslRequired The flag that indicates if SSL is required.
     * @param accessPrecluded The flag that indicates if access is precluded.
     * @param collectionMatch The web resource collection match.
     */
    public MatchResponse(List<String> roles, boolean sslRequired, boolean accessPrecluded, CollectionMatch collectionMatch) {
        this(roles, sslRequired, accessPrecluded);
        this.collectionMatch = collectionMatch;
    }

    /**
     * Gets the roles.
     */
    public List<String> getRoles() {
        return roles;
    }

    /**
     * Returns if SSL is required.
     * SSL is required if transport-guarantee is other than NONE.
     */
    public boolean isSSLRequired() {
        return sslRequired;
    }

    /**
     * Returns if access is precluded.
     * Access is precluded when there is an auth-constraint element, but there are no roles.
     */
    public boolean isAccessPrecluded() {
        return accessPrecluded;
    }

    /**
     * This method will aggregate the responses based on this match response type.
     * <pre>
     * To aggregate the response,
     * 
     * 1. If exact match, then merge response with previous exact match responses.
     * 2. Else if path match with longest URL pattern, then override previous path match responses.
     * 3. Else if path match with same length as longest URL pattern found, then merge with previous path match responses.
     * 4. Else if extension match, then merge with previous extension match responses.
     * </pre>
     * 
     * @param responseAggregate
     * @return The aggregated responses.
     */
    public ResponseAggregate aggregateResponse(ResponseAggregate responseAggregate) {
        if (collectionMatch.isExactMatch()) {
            MatchResponse exactMatchResponse = merge(responseAggregate.getExactMatchResponse());;
            responseAggregate.setExactMatchResponse(exactMatchResponse);
        } else if (collectionMatch.isPathMatch() && collectionMatch.getUrlPattern().length() > responseAggregate.getLongestPathUrlPattern().length()) {
            responseAggregate.setPathMatchResponse(this);
            responseAggregate.setLongestPathUrlPattern(collectionMatch.getUrlPattern());
        } else if (collectionMatch.isPathMatch() && collectionMatch.getUrlPattern().length() == responseAggregate.getLongestPathUrlPattern().length()) {
            MatchResponse pathMatchResponse = merge(responseAggregate.getPathMatchResponse());
            responseAggregate.setPathMatchResponse(pathMatchResponse);
            responseAggregate.setLongestPathUrlPattern(collectionMatch.getUrlPattern());
        } else if (collectionMatch.isExtensionMatch()) {
            MatchResponse extensionMatchResponse = merge(responseAggregate.getExtensionMatchResponse());
            responseAggregate.setExtensionMatchResponse(extensionMatchResponse);
        } else if (collectionMatch.isDenyMatch()) {
            MatchResponse denyMatchResponse = merge(responseAggregate.getDenyMatchResponse());
            responseAggregate.setDenyMatchResponse(denyMatchResponse);
        } else if (collectionMatch.isDenyMatchByOmission()) {
            MatchResponse denyMatchByOmissionResponse = merge(responseAggregate.getDenyMatchByOmissionResponse());
            responseAggregate.setDenyMatchByOmissionResponse(denyMatchByOmissionResponse);
        }
        return responseAggregate;
    }

    public CollectionMatch getCollectionMatch() {
        return collectionMatch;
    }

    /**
     * Merges the roles, sslRequired, and accessPrecluded fields according to the
     * Servlet 2.3 and 3.0 specifications.
     * 
     * @param matchResponse
     * @return
     */
    public MatchResponse merge(MatchResponse matchResponse) {
        if (matchResponse == null || matchResponse == this) {
            return this;
        } else {
            boolean mergedSSLRequired = mergeSSLRequired(matchResponse.isSSLRequired());
            boolean mergedAccessPrecluded = mergeAccessPrecluded(matchResponse.isAccessPrecluded());
            List<String> mergedRoles = mergeRoles(matchResponse.getRoles(), mergedAccessPrecluded);
            return new MatchResponse(mergedRoles, mergedSSLRequired, mergedAccessPrecluded);
        }
    }

    /**
     * Merges the sslRequired fields.
     * 
     * For exact match, SSL not required takes precedence.
     * For path matches of equal length and extension match, SSL required takes precedence.
     * 
     * @param otherSSLRequired
     * @return
     */
    protected boolean mergeSSLRequired(boolean otherSSLRequired) {
        boolean mergedSSLRequired = false;
        if (collectionMatch.isExactMatch()) {
            mergedSSLRequired = sslRequired && otherSSLRequired;
        } else if (collectionMatch.isPathMatch() || collectionMatch.isExtensionMatch()) {
            mergedSSLRequired = sslRequired || otherSSLRequired;
        }
        return mergedSSLRequired;
    }

    /**
     * Access precluded takes precedence over access not precluded.
     */
    protected boolean mergeAccessPrecluded(boolean otherAccessPrecluded) {
        return accessPrecluded || otherAccessPrecluded;
    }

    /**
     * Based on the Servlet 3.0 specs, precluded takes precedence, and then
     * no roles precedes any role mapping.
     * <pre>
     * 1. Precluded
     * 2. No roles
     * 3. Specified roles
     * </pre>
     * 
     * @param otherRoles
     * @param mergedAccessPrecluded
     * @return The merged roles.
     */
    @SuppressWarnings("unchecked")
    protected List<String> mergeRoles(List<String> otherRoles, boolean mergedAccessPrecluded) {
        if (mergedAccessPrecluded == true || roles.isEmpty() || otherRoles.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        Set<String> tempRoles = new HashSet<String>();
        tempRoles.addAll(otherRoles);
        tempRoles.addAll(roles);
        List<String> mergedRoles = new ArrayList<String>();
        mergedRoles.addAll(tempRoles);
        return mergedRoles;
    }
}
