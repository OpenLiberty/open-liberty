/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.jaxws.ejbinterceptor;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.jws.WebService;

@Stateless
@WebService
@Interceptors(SayHelloInterceptorWithException.class)
public class SayHelloWithInterceptorException {

    public String hello(String userName) {
        return "hello, " + userName;
    }
}
