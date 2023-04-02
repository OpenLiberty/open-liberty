/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.cdi20.fat.apps.cdiContainerConfig.web;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.ibm.ws.cdi20.fat.apps.cdiContainerConfig.explicit.MyExplicitBean;
import com.ibm.ws.cdi20.fat.apps.cdiContainerConfig.implicit.MyImplicitBean;

@RequestScoped
public class MyBeanCDI20 {

    @Inject
    Instance<MyImplicitBean> implicit;

    @Inject
    Instance<MyExplicitBean> explicit;

    public boolean isImplicitUnsatisfied() {
        return implicit.isUnsatisfied();
    }

    public boolean isExplicitUnsatisfied() {
        return explicit.isUnsatisfied();
    }

}
