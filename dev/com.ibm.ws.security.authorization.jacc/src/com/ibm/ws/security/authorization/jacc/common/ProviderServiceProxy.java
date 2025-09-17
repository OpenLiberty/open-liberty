/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.authorization.jacc.common;

import javax.security.jacc.PolicyConfigurationFactory;

import com.ibm.ws.security.authorization.jacc.PolicyConfigurationManager;

public interface ProviderServiceProxy {

    /**
     * Returns the PolicyProxy instance representing the provider-specific implementation
     * of the java.security.Policy abstract class or jakarta.security.jacc.Policy interface.
     *
     * @return An instance which implements PolicyProxy interface.
     */
    public PolicyProxy getPolicyProxy(PolicyConfigurationManager pcm);

    /**
     * Returns the instance representing the provider-specific implementation
     * of the javax.security.jacc.PolicyConfigurationFactory abstract class.
     *
     * @return An instance which implements PolicyConfigurationFactory class.
     */
    public PolicyConfigurationFactory getPolicyConfigFactory();

    /**
     * Returns the property from the underlying ProviderService that this object is a proxy to.
     *
     * @param name of the property requested.
     * @return value of the property requested.
     */
    public Object getProperty(String property);

    public String getPolicyName();

    public String getFactoryName();
}
