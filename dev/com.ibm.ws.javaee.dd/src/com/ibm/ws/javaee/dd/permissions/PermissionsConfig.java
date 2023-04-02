/*******************************************************************************
 * Copyright (c) 2015,2022 IBM Corporation and others.
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
package com.ibm.ws.javaee.dd.permissions;

import java.util.List;

import com.ibm.ws.javaee.dd.DeploymentDescriptor;

public interface PermissionsConfig extends DeploymentDescriptor {
    String DD_SHORT_NAME = "permissions.xml";
    String DD_NAME = "META-INF/permissions.xml";

    int VERSION_7_0  =  70;
    int VERSION_9_0  =  90;
    int VERSION_10_0 = 100;
    
    String VERSION_7_STR  =  "7";
    String VERSION_9_STR  =  "9";
    String VERSION_10_STR = "10";

    List<Permission> getPermissions();
}
