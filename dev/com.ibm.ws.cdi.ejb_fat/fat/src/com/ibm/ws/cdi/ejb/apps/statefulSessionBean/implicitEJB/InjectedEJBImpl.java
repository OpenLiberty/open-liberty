/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.ejb.apps.statefulSessionBean.implicitEJB;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Stateful
@Dependent
public class InjectedEJBImpl implements InjectedEJB {

    private static final long serialVersionUID = 1L;

    @Inject
    private InjectedBean2 bean;

    public InjectedEJBImpl() {
        System.out.println("xtor");
    }

    @Override
    public String getData() {
        return "STATE" + bean.increment();
    }

    @Override
    @Remove
    public void removeEJB() {
        System.out.println("REMOVE");
    }

}
