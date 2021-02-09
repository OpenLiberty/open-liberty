/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.client.fat;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.validation.ValidatorFactory;

import com.ibm.ws.security.client.fat.view.My2SimpleInjectionBeanRemote;

/**
 * Session Bean implementation class MySimpleInjectionBean
 */
@Singleton
@Startup
@Remote(My2SimpleInjectionBeanRemote.class)
public class My2SimpleInjectionBean implements My2SimpleInjectionBeanRemote {

    @Resource(name="java:global/env/myValidatorFactory")
    ValidatorFactory validatorFactory;

    /**
     * Default constructor. 
     */
    public My2SimpleInjectionBean() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public int add(int x, int y) {
        return x+y;
    }

}

