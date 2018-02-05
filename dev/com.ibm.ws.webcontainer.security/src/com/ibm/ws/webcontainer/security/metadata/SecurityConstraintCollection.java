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

import java.util.List;

/**
 * Represents a collection of security constraint objects.
 * An instance of this interface is the main object to determine what constraints match
 * the given resource access, where the constraints are represented by the MatchResponse
 * object and it contains the roles, if SSL is required, and if access is precluded for such access.
 */
public interface SecurityConstraintCollection {

    /**
     * Gets the match response object for the resource access.
     *
     * @param resourceName The resource name.
     * @param method The HTTP method.
     * @return The MatchResponse object.
     */
    public abstract MatchResponse getMatchResponse(String resourceName, String method);

    /**
     * Gets the match response object for the resource access.
     *
     * @param resourceName The resource name.
     * @param method The HTTP method.
     * @return The MatchResponse object.
     */
    public abstract List<MatchResponse> getMatchResponse(String resourceName, String... method);

    /**
     * Gets the list of security constraints in this collection
     *
     * @return a list of SecurityConstraint objects
     */
    public List<SecurityConstraint> getSecurityConstraints();

    /**
     * Adds the given security constraints to the collection
     *
     * @param securityConstraints the security constraints to add
     */
    public void addSecurityConstraints(List<SecurityConstraint> securityConstraints);

}