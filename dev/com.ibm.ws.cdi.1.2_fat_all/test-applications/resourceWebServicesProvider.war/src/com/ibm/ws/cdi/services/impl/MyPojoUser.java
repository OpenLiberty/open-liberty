/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.services.impl;

import javax.enterprise.context.Dependent;

/**
 * This class only needs to be here to make sure this is a BDA with a BeanManager
 */
@Dependent
public class MyPojoUser {

    public String getUser() {
        String s = "DefaultPojoUser";
        return s;
    }
}
