/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.cdi.beans;

import javax.annotation.Priority;
import javax.interceptor.Interceptor;
import javax.security.enterprise.authentication.mechanism.http.RememberMe;

/**
 * TODO: Determine how it intercepts the HttpAuthenticationMechanism and calls the RememberMeIdentityStore bean provided by the application
 */
@RememberMe
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE)
public class RememberMeInterceptor {

    // TODO: Add @AroundInvoke method
}
