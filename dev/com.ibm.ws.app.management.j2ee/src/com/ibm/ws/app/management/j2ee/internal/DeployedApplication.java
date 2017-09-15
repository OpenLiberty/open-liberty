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
package com.ibm.ws.app.management.j2ee.internal;

import javax.management.ObjectName;

import com.ibm.websphere.management.j2ee.J2EEApplication;
import com.ibm.websphere.management.j2ee.J2EEApplicationMBean;
import com.ibm.ws.javaee.ddmodel.DDReader;
import com.ibm.wsspi.adaptable.module.Container;

public class DeployedApplication extends J2EEApplication implements J2EEApplicationMBean {
    private final ObjectName[] moduleObjectNames;
    private final Container container;
    private final String ddPath;

    public DeployedApplication(ObjectName objectName,
                               ObjectName serverObjectName,
                               Container container,
                               String ddPath,
                               ObjectName[] moduleObjectNames) {
        super(objectName, serverObjectName);
        this.container = container;
        this.ddPath = ddPath;
        this.moduleObjectNames = moduleObjectNames;
    }

    @Override
    public String getdeploymentDescriptor() {
        return DDReader.read(container, ddPath);
    }

    @Override
    public String[] getmodules() {
        String[] ejbs = new String[moduleObjectNames.length];
        for (int i = 0; i < moduleObjectNames.length; i++) {
            ejbs[i] = moduleObjectNames[i].toString();
        }
        return ejbs;
    }
}
