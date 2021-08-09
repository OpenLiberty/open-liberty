/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.ejbinterceptor;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.jws.WebService;

@Stateless
@WebService
@Interceptors(SayHelloInterceptor.class)
public class SayHello {

    public String hello(String userName) {
        return "hello, " + userName;
    }
}
