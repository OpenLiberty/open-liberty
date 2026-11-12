/*******************************************************************************
 * Copyright (c) 2012, 2026 IBM Corporation and others.
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

package com.ibm.ws.ejb.session;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

@Stateless
public class BasicStatelessBean {

    @Resource
    SessionContext ejbcontext;

    /**
     * Secured method that only accepts calls from users under role1 only.
     */
    @RolesAllowed("role1")
    public String role1() {
        return "role1SuccessfulCallToMethod";
    }
    
    /**
     * Secured method that only accepts calls from users under role2 only.
     */
    @RolesAllowed("role2")
    public String role2() {
        return "role2SuccessfulCallToMethod";
    }
    
    @RolesAllowed("role3")
    public String role3() {
        return "role3SuccessfulCallToMethod";
    }
    
    /**
     * Unsecured method that accept calls from any user.
     * @return
     */
    public String unsecured() {
        return "successfulCallToUnsecuredMethod";
    }
}
