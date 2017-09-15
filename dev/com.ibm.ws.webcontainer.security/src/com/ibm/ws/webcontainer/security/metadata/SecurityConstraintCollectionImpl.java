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
 */
public class SecurityConstraintCollectionImpl implements SecurityConstraintCollection {

    private final List<SecurityConstraint> securityConstraints;

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

    public void addSecurityConstraints(List<SecurityConstraint> securityConstraints) {
        this.securityConstraints.addAll(securityConstraints);
    }

}
