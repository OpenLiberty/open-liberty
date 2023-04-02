/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.cdi.beansxml.fat.apps.aftertypediscovery;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class InterceptedBean {

    @InterceptedAfterType
    public InterceptedBean() {
        System.out.println("InterceptedBean.InterceptedBean()");
    }

    public void doNothing() {
        System.out.println("InterceptedBean.doNothing()");
    }
}
