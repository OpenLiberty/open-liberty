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

/**
 * <p>
 * This class is for generic Websphere runtime permissions. A <code>WebSphereRuntimePermission</code>
 * contains a name (also referred to as a "target name") but no action list, either the
 * permission is granted or not.
 * </p>
 * 
 * <p>
 * The target name is the name of a security configuration parameter. Currently
 * the <code>WebSphereRuntimePermission</code> used to guard access to the following objects:
 * <ul>
 * <li><code>SecurityCallbackHandlerAccessor</code></li>
 * </ul>
 * </p>
 * 
 * <p>
 * Possible target names for WebSphere runtime permissions are:
 * <ul>
 * <li>
 * <pre>setClientContainerCallback - allow the caller to invoke the
 * <code>SecurityCallbackHandlerAccessor.setCallbackHandler</code> method</pre>
 * </li>
 * </ul>
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since 1.0
 */
public final class WebSphereRuntimePermission extends java.security.BasicPermission {

    private static final long serialVersionUID = -3306242789188168337L; //@vj1: Take versioning into account if incompatible changes are made to this class

    /**
     * <p>
     * Creates a new <code>WebSphereRuntimePermission</code> with the specified name.
     * The name is the symbolic name of the <code>WebSphereRuntimePermission</code>.
     * </p>
     * 
     * @param target The name of the <code>WebSphereRuntimePermission</code>.
     */
    public WebSphereRuntimePermission(String target) {
        super(target);
    }

    /**
     * <p>
     * Creates a new <code>WebSphereRuntimePermission</code> with the specified name.
     * The name is the symbolic name of the <code>WebSphereRuntimePermission</code>,
     * and the actions <code>String</code> is currently unused and should be
     * <code>null</code>. This constuctor exists for use by <code>Policy</code> object
     * to instantiate new Permission objects.
     * </p>
     * 
     * @param target The name of the <code>WebSphereRuntimePermission</code>.
     * @param actions Should be <code>null</code>.
     * @see java.security.Policy
     */
    public WebSphereRuntimePermission(String target, String actions) {
        super(target, null);
    }
}
