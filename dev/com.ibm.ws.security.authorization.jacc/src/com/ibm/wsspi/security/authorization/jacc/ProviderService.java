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

package com.ibm.wsspi.security.authorization.jacc;
import java.security.Policy;
import javax.security.jacc.PolicyConfigurationFactory;

public interface ProviderService {
    /**
     * Returns the instance representing the provider-specific implementation 
     * of the java.security.Policy abstract class. 
     * @return An instance which implements java.security.Policy class.
     */
    public Policy getPolicy();
    /**
     * Returns the instance representing the provider-specific implementation
     * of the javax.security.jacc.PolicyConfigurationFactory abstract class.
     * @return An instance which implements PolicyConfigurationFactory class.
     */
    public PolicyConfigurationFactory getPolicyConfigFactory();
}
