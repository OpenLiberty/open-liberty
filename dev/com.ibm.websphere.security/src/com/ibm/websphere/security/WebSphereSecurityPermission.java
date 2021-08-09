/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.security;

import java.security.Permission;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;

/**
 * <p>
 * This class is for generic Websphere security permissions. A <code>WebSphereSecurityPermission</code>
 * contains a name (also referred to as a "target name") but with no an action list. The following names are allowed:
 * internal, provider, privileged, where internal implies provider, and provider implies privileged.
 * </p>
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since 1.0
 */

public final class WebSphereSecurityPermission extends java.security.BasicPermission {

    private static final long serialVersionUID = 7885831460901050879L; //@vj1: Take versioning into account if incompatible changes are made to this class

    private final static TraceComponent tc = Tr.register(com.ibm.websphere.security.WebSphereSecurityPermission.class);
    public final static WebSphereSecurityPermission INTERNAL_PERMISSION = new WebSphereSecurityPermission("internal");
    public final static WebSphereSecurityPermission PROVIDER_PERMISSION = new WebSphereSecurityPermission("provider");
    public final static WebSphereSecurityPermission PRIVILEGED_PERMISSION = new WebSphereSecurityPermission("privileged");
    /**
     * INTERNAL permission.
     */
    private final static int INTERNAL = 0x4;
    /**
     * Provider permission.
     */
    private final static int PROVIDER = 0x2;
    /**
     * Privileged permission.
     */
    private final static int PRIVILEGED = 0x1;

    /**
     * No permission.
     */
    private final static int NONE = 0x0;

    // the permission max
    private transient int max;

    /**
     * the target string.
     * 
     * @serial
     */
    private final static String INTERNAL_STR = "internal";
    private final static String PROVIDER_STR = "provider";
    private final static String PRIVILEGED_STR = "privileged";

    /**
     * initialize a WebSphereSecurityPermission object. Common to all constructors.
     * 
     * @param max the most privileged permission to use.
     * 
     */
    private void init(int max) {
        if ((max != INTERNAL) && (max != PROVIDER) && (max != PRIVILEGED))
            throw new IllegalArgumentException("invalid action");

        if (max == NONE)
            throw new IllegalArgumentException("missing action");

        if (getName() == null)
            throw new NullPointerException("action can't be null");

        this.max = max;
    }

    /**
     * <p>
     * Creates a new <code>WebSphereSecurityPermission</code> with the default name of
     * "WebSphereSecurityPermission" and an action.
     * The name is the symbolic name of the <code>WebSphereSecurityPermission</code>.
     * the following action values are valid: "internal", "provider", and "privileged".
     * </p>
     * <p>The internal permission implies the provider permission. The provider
     * permission implies the privileged permission. To maintain runtime integrity,
     * no application code should be given permission above privileged. The provider
     * permission should be granted to plug-in code. The internal permission is
     * granted only to WebSphere Application Server runtime code.
     * </p>
     * 
     * @param action The action value of the <code>WebSphereSecurityPermission</code>.
     */
    public WebSphereSecurityPermission(String action) {
        super("WebSphereSecurityPermission");
        init(getMax(action));
    }

    /*
     * public WebSphereSecurityPermission(String target, String actions) { //@vj1: Is Serialization affected by the removal of this constructor? It was present for WAS5X.
     * super(target, null);
     * init(getMax(target));
     * }
     */

    @Override
    public boolean implies(Permission p) {
        if (!(p instanceof WebSphereSecurityPermission))
            return false;

        WebSphereSecurityPermission that = (WebSphereSecurityPermission) p;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Permission " + this.max + "impliles " + that.max + " = " + (this.max > that.max));
        }

        return (this.max >= that.max);
    }

    /**
     * Converts an action String to a permission value.
     * 
     * @param action the action string.
     * @return the max permission.
     */
    private static int getMax(String action) {

        int max = NONE;

        if (action == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "WebSphereSecurityPermission action should not be null");
            }
            return max;
        }

        if (INTERNAL_STR.equalsIgnoreCase(action)) {
            max = INTERNAL;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "WebSphereSecurityPermission - internal");
            }
        } else if (PROVIDER_STR.equalsIgnoreCase(action)) {
            max = PROVIDER;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "WebSphereSecurityPermission - provider");
            }
        } else if (PRIVILEGED_STR.equalsIgnoreCase(action)) {
            max = PRIVILEGED;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "WebSphereSecurityPermission - privileged");
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "WebSphereSecurityPermission invalid action " + action);
            }
            throw new IllegalArgumentException(
                            "invalid permission: " + action);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "WebSphereSecurityPermission value = " + max);
        }
        return max;
    }
}
