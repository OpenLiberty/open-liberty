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
package com.ibm.ws.security.authorization.builtin.internal;

import java.util.Collection;
import java.util.Iterator;

import javax.security.auth.Subject;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.ws.security.authorization.AccessDecisionService;

/**
 * Built-in access decision service which implements a role-based authorization
 * model.
 */
public class BuiltinAccessDecisionService implements AccessDecisionService {
    private static final TraceComponent tc = Tr.register(BuiltinAccessDecisionService.class);

    private ServiceReference<JavaEEVersion> eeVersionRef;
    private volatile Version eeVersion = JavaEEVersion.DEFAULT_VERSION;

    // '**' will represent the Servlet 3.1 defined all authenticated Security constraint
    private static final String ALL_AUTHENTICATED_ROLE = "**";

    // '_starstar_' will represent if ** was specified as a defined role
    private static final String STARSTAR_ROLE = "_starstar_";

    public synchronized void setVersion(ServiceReference<JavaEEVersion> reference) {
        eeVersionRef = reference;
        eeVersion = Version.parseVersion((String) reference.getProperty("version"));
    }

    public synchronized void unsetVersion(ServiceReference<JavaEEVersion> reference) {
        if (reference == this.eeVersionRef) {
            eeVersionRef = null;
            eeVersion = JavaEEVersion.DEFAULT_VERSION;
        }
    }

    private boolean isEEVersion7()
    {
        if (eeVersion.compareTo(JavaEEVersion.VERSION_7_0) >= 0) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}<p>
     * The Subject is not actually used here as the assigned roles have
     * already been computed.
     */
    @Override
    public boolean isGranted(String resourceName, Collection<String> requiredRoles,
                             Collection<String> assignedRoles, Subject subject) {

        /*
         * New for Servlet 3.1 "**" means all authenticated users. The subject
         * is checked prior to isGranted() being called. So return true if the
         * subject is not null and if "**" is a required role.
         * 
         * If "_starstar_" is passed in that means a user had defined a role called
         * "**". We need to convert "_starstar_" back to "**" before the check
         * against assignedRoles.
         */
        if (subject != null && requiredRoles.contains(ALL_AUTHENTICATED_ROLE) && isEEVersion7())
            return true;

        if (assignedRoles != null) {

            Iterator<String> iter = requiredRoles.iterator();
            while (iter.hasNext()) {
                String r = iter.next();
                if (r.equals(STARSTAR_ROLE))
                    r = "**";
                if (assignedRoles.contains(r)) {
                    return true;
                }
            }
        }
        return false;
    }

}