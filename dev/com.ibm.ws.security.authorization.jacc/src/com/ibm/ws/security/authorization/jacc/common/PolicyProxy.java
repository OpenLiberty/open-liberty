/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.common;

import java.security.Permission;
import java.util.Set;

import javax.security.auth.Subject;

/**
 * Proxy interface to represent either a java.security.Policy or jakarta.security.jacc.Policy object
 */
public interface PolicyProxy {

    public void refresh(Set<String> contextIds);

    default public void setPolicy() {
        // Do nothing for EE 11+
    }

    public boolean implies(String contextId, Subject subject, Permission permission);

    default public boolean isResetPolicyContextID() {
        // For pre-EE 11 scenarios, return false to not change behavior
        return false;
    }

    /**
     * In a Jakarta EE 11+ implementation this method returns the PrincipalMapper implementation
     *
     * @return PrincipalMapper implementation
     */
    default public Object getPrincipalMapper() {
        // Default is to throws exception since it isn't expected to be called for pre-EE 11 scenarios
        throw new UnsupportedOperationException();
    }

    /**
     * Determines if a Policy is configured.  In EE 11, we create the PolicyProxy always even if there
     * isn't a PolicyFactory defined because it can be added dynamically by applications in their web.xml
     * or using the PolicyFactory.setPolicyFactory() method.
     */
    default public boolean isPolicyConfigured() {
        // return true for pre EE 11 versions since the PolicyProxy isn't created unless there is a Policy
        return true;
    }
}
