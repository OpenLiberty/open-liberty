/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdiContainerConfigApp.web;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import cdiContainerConfigApp.explicit.MyExplicitBean;
import cdiContainerConfigApp.implicit.MyImplicitBean;

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
