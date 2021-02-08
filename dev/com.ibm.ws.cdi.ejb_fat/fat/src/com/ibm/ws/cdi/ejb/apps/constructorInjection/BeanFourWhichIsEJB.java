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

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 *
 */
@Stateless
@LocalBean
@MyForthQualifier
public class BeanFourWhichIsEJB implements Iface {

    private final String msg = "eggs";

    @Override
    public String getMsg() {
        return msg;
    }

}
