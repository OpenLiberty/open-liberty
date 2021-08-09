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
package com.ibm.ws.cdi.ejb.apps.postConstructError;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;

import com.ibm.ws.cdi.ejb.apps.postConstructError.interceptors.ErrorMessageInterceptorBinding;

@Stateless
@ErrorMessageInterceptorBinding
public class ErrorMessageTestEjb {
    public ErrorMessageTestEjb() {} // necessary to be proxyable

    public void doSomething() {}

    @PostConstruct
    public void postConstructMethod() {}
}
