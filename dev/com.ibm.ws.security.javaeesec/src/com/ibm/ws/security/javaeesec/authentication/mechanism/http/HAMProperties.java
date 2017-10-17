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
package com.ibm.ws.security.javaeesec.authentication.mechanism.http;

import java.util.Properties;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public interface HAMProperties {
    /**
     * returns the implementation class name of HttpAuthenticationMechansim
     */
    public Class getImplementationClass();

    /**
     * returns the properties which is associated with the implementation class.
     * For BasicAuthenticationMechanism, "realmName".
     * For CustomForm and Form AuthenticationMechanism, "errorPage", "loginPage", "useForwardToLogin", "useForwardtoLoginExpression"
     */
    public Properties getProperties();
}
