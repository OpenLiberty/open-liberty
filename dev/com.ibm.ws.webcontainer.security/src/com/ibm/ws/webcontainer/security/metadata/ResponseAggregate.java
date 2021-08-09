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

/**
 * This class is to be used when aggregating the responses from the SecurityContraint objects in
 * the SecurityConstraintCollection.
 */
public class ResponseAggregate {

    private MatchResponse defaultMatchResponse;
    private MatchResponse exactMatchResponse = null;
    private MatchResponse pathMatchResponse = null;
    private MatchResponse extensionMatchResponse = null;
    private MatchResponse denyMatchResponse = null;
    private MatchResponse denyMatchByOmissionResponse = null;
    private final MatchResponse permitResponse = null;
    private String longestPathUrlPattern = "";

    /**
     * Constructs a ResponseAggregate with a default match response.
     * 
     * @param defaultMatchResponse
     */
    public ResponseAggregate(MatchResponse defaultMatchResponse) {
        this.defaultMatchResponse = defaultMatchResponse;
    }

    public void setExactMatchResponse(MatchResponse exactMatchResponse) {
        this.exactMatchResponse = exactMatchResponse;
    }

    public MatchResponse getExactMatchResponse() {
        return exactMatchResponse;
    }

    public void setPathMatchResponse(MatchResponse pathMatchResponse) {
        this.pathMatchResponse = pathMatchResponse;
    }

    public MatchResponse getPathMatchResponse() {
        return pathMatchResponse;
    }

    public void setExtensionMatchResponse(MatchResponse extensionMatchResponse) {
        this.extensionMatchResponse = extensionMatchResponse;
    }

    public MatchResponse getExtensionMatchResponse() {
        return extensionMatchResponse;
    }

    public void setDenyMatchResponse(MatchResponse denyMatchResponse) {
        this.denyMatchResponse = denyMatchResponse;
    }

    public MatchResponse getDenyMatchResponse() {
        return denyMatchResponse;
    }

    public void setDenyMatchByOmissionResponse(MatchResponse denyMatchByOmissionResponse) {
        this.denyMatchByOmissionResponse = denyMatchByOmissionResponse;
    }

    public MatchResponse getDenyMatchByOmissionResponse() {
        return denyMatchByOmissionResponse;
    }

    public MatchResponse getPermitResponse() {
        return permitResponse;
    }

    public void setLongestPathUrlPattern(String longestPathUrlPattern) {
        this.longestPathUrlPattern = longestPathUrlPattern;
    }

    public String getLongestPathUrlPattern() {
        return longestPathUrlPattern;
    }

    /**
     * Selects the match response from this aggregate.
     * <pre>
     * To select the match response according to the Servlet 2.3 specification,
     * Exact match has first priority.
     * Path match comes next.
     * Extension match comes after path match.
     * No Match comes last.
     * </pre>
     * 
     * @return The match response.
     */
    public MatchResponse selectMatchResponse() {
        MatchResponse matchResponse = null;

        if (permitResponse != null && denyMatchByOmissionResponse == null && denyMatchResponse == null) {
            matchResponse = exactMatchResponse;
        } else if (exactMatchResponse != null && denyMatchByOmissionResponse != null && denyMatchResponse != null) {
            matchResponse = exactMatchResponse; 
        } else if (exactMatchResponse != null && denyMatchByOmissionResponse != null) {
            matchResponse = exactMatchResponse;
        } else if (exactMatchResponse != null && denyMatchResponse != null) {
            matchResponse = exactMatchResponse; 
        } else if (exactMatchResponse != null && denyMatchByOmissionResponse == null && denyMatchResponse == null) {
            matchResponse = exactMatchResponse;
        } else if (pathMatchResponse != null) {
            matchResponse = pathMatchResponse;
        } else if (extensionMatchResponse != null) {
            matchResponse = extensionMatchResponse;
        } else if (denyMatchResponse != null) {
            matchResponse = MatchResponse.DENY_MATCH_RESPONSE;
        } else if (denyMatchByOmissionResponse != null && exactMatchResponse == null) {
            matchResponse = MatchResponse.DENY_MATCH_RESPONSE;
        } else {
            matchResponse = defaultMatchResponse;
        }

        return matchResponse;
    }

    public void setDefaultResponse(MatchResponse defaultMatchResponse) {
        this.defaultMatchResponse = defaultMatchResponse;
    }

}
