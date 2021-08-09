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
@MyQualifier
public class BeanOne implements Iface {

    private final String msg = "foo";

    @Override
    public String getMsg() {
        return msg;
    }

}
