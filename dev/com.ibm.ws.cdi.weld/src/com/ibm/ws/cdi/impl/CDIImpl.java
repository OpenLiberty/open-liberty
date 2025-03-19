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
package com.ibm.ws.cdi.impl;

import javax.enterprise.inject.spi.BeanManager;

import org.jboss.weld.AbstractCDI;

import com.ibm.ws.cdi.internal.interfaces.CDIRuntime;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;

/**
 * IBM implementation of CDI
 */
public class CDIImpl extends AbstractCDI<Object> {
    private final CDIRuntime cdiRuntime;
    private final WebSphereCDIDeployment parent;

    public CDIImpl(CDIRuntime cdiRuntime, WebSphereCDIDeployment parent) {
        this.cdiRuntime = cdiRuntime;
        this.parent = parent;
    }

    /** {@inheritDoc} */
    @Override
    public BeanManager getBeanManager() {
        //TOOD is there a reason why this isn't on the interface?
        CDIContainerImpl cdiContainer = (CDIContainerImpl) cdiRuntime.getCDIContainer();
        return cdiContainer.getCurrentBeanManager(parent);
    }

    public static void clear() {
        configuredProvider = null;
    }
}
