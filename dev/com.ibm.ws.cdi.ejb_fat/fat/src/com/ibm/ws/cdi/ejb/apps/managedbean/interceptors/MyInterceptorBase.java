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
package com.ibm.ws.cdi.ejb.apps.managedbean.interceptors;

import javax.ejb.EJB;

import com.ibm.ws.cdi.ejb.apps.managedbean.MyEJBBeanLocal;

/**
 *
 */
public class MyInterceptorBase {

    @EJB
    private MyEJBBeanLocal ejbBean;

    public String getAroundInvokeText() {
        System.out.println("getAroundInvokeText");
        return "AroundInvoke";
    }

}
