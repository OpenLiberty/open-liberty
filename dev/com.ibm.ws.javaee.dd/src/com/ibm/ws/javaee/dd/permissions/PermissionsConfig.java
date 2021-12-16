/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.permissions;

import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;

/**
 *
 */
public interface PermissionsConfig extends DeploymentDescriptor {

    static final String DD_NAME = "META-INF/permissions.xml";

    static final int VERSION_7_0 = 70;

    /**
     * @return &lt;Permission> as a read-only list
     */
    List<Permission> getPermissions();

}
