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

public class J2EEManagedObject implements J2EEManagedObjectMBean {
    private final ObjectName objectName;

    public J2EEManagedObject(ObjectName objectName) {
        this.objectName = objectName;
    }

    @Override
    public String getobjectName() {
        return objectName.toString();
    }

    @Override
    public boolean isstateManageable() {
        return false;
    }

    @Override
    public boolean isstatisticsProvider() {
        return false;
    }

    @Override
    public boolean iseventProvider() {
        return false;
    }
}
