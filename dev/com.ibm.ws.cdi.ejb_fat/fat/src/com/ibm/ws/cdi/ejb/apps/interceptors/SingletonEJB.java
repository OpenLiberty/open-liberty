/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.ejb.apps.interceptors;

import javax.ejb.Singleton;
import javax.interceptor.Interceptors;

@Singleton
@Interceptors({ InterceptorSubClass2.class, InterceptorSubClass1.class })
public class SingletonEJB {

    public SingletonEJB() {
        System.out.println("SingletonEJB xtor");
    }

    public String message() {
        System.out.println("SingletonEJB message - " + this.hashCode());
        return "Hello";
    }

}
