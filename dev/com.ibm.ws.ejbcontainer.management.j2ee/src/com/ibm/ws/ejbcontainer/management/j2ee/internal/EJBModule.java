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
package com.ibm.ws.ejbcontainer.management.j2ee.internal;

import javax.management.ObjectName;

import com.ibm.websphere.management.j2ee.EJBModuleMBean;
import com.ibm.websphere.management.j2ee.J2EEModule;
import com.ibm.ws.javaee.ddmodel.DDReader;
import com.ibm.wsspi.adaptable.module.Container;

public class EJBModule extends J2EEModule implements EJBModuleMBean {
    private final ObjectName[] ejbObjectNames;
    private final Container container;
    private final String ddPath;

    public EJBModule(ObjectName objectName,
                     ObjectName serverObjectName,
                     ObjectName jvmObjectName,
                     Container container,
                     String ddPath,
                     ObjectName[] ejbObjectNames) {
        super(objectName, serverObjectName, jvmObjectName);
        this.container = container;
        this.ddPath = ddPath;
        this.ejbObjectNames = ejbObjectNames;
    }

    @Override
    public String getdeploymentDescriptor() {
        return DDReader.read(container, ddPath);
    }

    @Override
    public String[] getejbs() {
        String[] ejbs = new String[ejbObjectNames.length];
        for (int i = 0; i < ejbObjectNames.length; i++) {
            ejbs[i] = ejbObjectNames[i].toString();
        }
        return ejbs;
    }
}
