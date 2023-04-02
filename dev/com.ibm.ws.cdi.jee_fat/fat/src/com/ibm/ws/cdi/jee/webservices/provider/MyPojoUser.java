/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.cdi.jee.webservices.provider;

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
