/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.wsspi.security.authorization.jacc;

import jakarta.security.jacc.Policy;
import jakarta.security.jacc.PolicyConfigurationFactory;

public interface ProviderService {

    /**
     * Returns the instance representing the provider-specific implementation
     * of the jakarta.security.jacc.Policy interface associated with the provided
     * PolicyContext.
     *
     * @param policy context ID that is associated with the returned Policy
     * @return An instance which implements the Policy interface.
     */
    public Policy getPolicy(String contextId);

    /**
     * Returns the instance representing the provider-specific implementation
     * of the jakarta.security.jacc.PolicyConfigurationFactory abstract class.
     *
     * @return An instance which implements PolicyConfigurationFactory class.
     */
    public PolicyConfigurationFactory getPolicyConfigFactory();
}
