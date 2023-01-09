/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.websphere.management.j2ee;

import javax.management.ObjectName;

public abstract class J2EEModule extends J2EEDeployedObject implements J2EEModuleMBean {
    private final ObjectName javaVMObjectName;

    public J2EEModule(ObjectName objectName, ObjectName serverObjectName, ObjectName javaVMObjectName) {
        super(objectName, serverObjectName);
        this.javaVMObjectName = javaVMObjectName;
    }

    @Override
    public String[] getjavaVMs() {
        return new String[] { javaVMObjectName.toString() };
    }
}
