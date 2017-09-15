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
package com.ibm.websphere.management.j2ee;

import javax.management.ObjectName;

public abstract class J2EEDeployedObject extends J2EEManagedObject implements J2EEDeployedObjectMBean {
    private final ObjectName serverObjectName;

    J2EEDeployedObject(ObjectName objectName, ObjectName serverObjectName) {
        super(objectName);
        this.serverObjectName = serverObjectName;
    }

    @Override
    public abstract String getdeploymentDescriptor();

    @Override
    public String getserver() {
        return serverObjectName.toString();
    }
}
