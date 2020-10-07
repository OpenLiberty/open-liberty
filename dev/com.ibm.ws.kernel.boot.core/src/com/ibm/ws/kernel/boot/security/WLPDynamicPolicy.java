/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.security;

import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class WLPDynamicPolicy extends Policy {

    // The policy that this WLPDynamicPolicy is replacing
    private Policy policy;

    // The set of URLs to give AllPermissions to; this is the set of bootURLs
    private List<URL> urls;

    // The AllPermissions collection
    private PermissionCollection allPermissions;

    // The AllPermission permission
    Permission allPermission = new AllPermission();

    private PermissionsCombiner permissionsCombiner;

    public WLPDynamicPolicy(Policy policy, List<URL> urls) {
        this.policy = policy;
        this.urls = urls;

        allPermissions = new PermissionCollection() {
            private static final long serialVersionUID = 3258131349494708277L;

            // A simple PermissionCollection that only has AllPermission
            @Override
            public void add(Permission permission) {
                //no adding to this policy
            }

            @Override
            public boolean implies(Permission permission) {
                return true;
            }

            @Override
            public Enumeration elements() {
                return new Enumeration() {
                    int cur = 0;

                    @Override
                    public boolean hasMoreElements() {
                        return cur < 1;
                    }

                    @Override
                    public Object nextElement() {
                        if (cur == 0) {
                            cur = 1;
                            return allPermission;
                        }
                        throw new NoSuchElementException();
                    }
                };
            }
        };
    }

    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        if (contains(codesource)) {
            return allPermissions;
        } else {
            return policy == null ? allPermissions : getMergedPermissions(codesource);
        }
    }

    private PermissionCollection getMergedPermissions(CodeSource codesource) {
        if (permissionsCombiner != null && codesource != null && codesource.getLocation() != null) {
            //return permissionsCombiner.getCombinedPermissions(new Permissions(), codesource); // TODO: Determine if this needs merging with the static permissions.
            return permissionsCombiner.getCombinedPermissions(policy.getPermissions(codesource), codesource); // TODO: Determine if this needs merging with the static permissions.
        } else {
            return policy.getPermissions(codesource);
        }
    }

    @Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        if (contains(domain.getCodeSource())) {
            return allPermissions;
        } else {
            return policy == null ? allPermissions : policy.getPermissions(domain);

        }
    }

    @Override
    public boolean implies(ProtectionDomain domain, Permission permission) {
        if (contains(domain.getCodeSource())) {
            return true;
        } else if (policy == null || policy.implies(domain, permission)) {
            return true;
        } else {
            // Special fallback case for JDK modules that have missing permissions
            // By adding modules here, we are effectively granting AllPermissions to those JDK modules
            String location = (domain != null && domain.getCodeSource() != null && domain.getCodeSource().getLocation() != null) //
                            ? domain.getCodeSource().getLocation().toExternalForm() : "";
            // Added because of https://github.com/eclipse/openj9/issues/6119
            return location.startsWith("jrt:/jdk.attach");
        }
    }

    @Override
    public void refresh() {
        if (policy != null)
            policy.refresh();
    }

    private boolean contains(CodeSource codeSource) {
        if (codeSource == null)
            return false;
        URL url = codeSource.getLocation();
        if (url == null)
            return false;
        // Check to see if this URL is in our set of URLs to give AllPermissions to.

        for (Iterator iter = urls.iterator(); iter.hasNext();) {
            URL u = (URL) iter.next();

            if (u.toString().equals(url.toString())) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param permissionsCombiner
     */
    public void setPermissionsCombiner(PermissionsCombiner permissionsCombiner) {
        this.permissionsCombiner = permissionsCombiner;
    }
}