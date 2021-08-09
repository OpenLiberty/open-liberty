package com.ibm.ws.cdi.ejb.apps.constructorInjection;

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

import javax.enterprise.context.RequestScoped;

/**
 *
 */
@RequestScoped
@MySecondQualifier
public class BeanTwo implements Iface {

    private final String msg = "bar";

    @Override
    public String getMsg() {
        return msg;
    }

}
