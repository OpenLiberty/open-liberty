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

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

@SessionScoped
public class InjectedBean1 implements Serializable {

    private static final long serialVersionUID = 1L;

    //TODO currently using @Inject doesn't behave the same as using @EJB
    @Inject
    private InjectedEJB ejb;

    public String getData() {
        return ejb.getData();
    }

    public void removeEJB() {
        ejb.removeEJB();
    }

}
