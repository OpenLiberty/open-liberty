/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.authorization.jacc.internal.web;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.security.jacc.PolicyConfigurationFactory;
import jakarta.security.jacc.PolicyFactory;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class AuthorizationServletListener implements ServletContextListener {

    private final PolicyFactory policyFactory;
    private final PolicyConfigurationFactory configFactory;
    private final PolicyFactory previousPolicyFactory;
    private final PolicyConfigurationFactory previousConfigFactory;

    AuthorizationServletListener(PolicyFactory policyFactory, PolicyConfigurationFactory configFactory, PolicyFactory previousPolicyFactory,
                                 PolicyConfigurationFactory previousConfigFactory) {
        this.policyFactory = policyFactory;
        this.configFactory = configFactory;
        this.previousPolicyFactory = previousPolicyFactory;
        this.previousConfigFactory = previousConfigFactory;
    }

    @Override
    @FFDCIgnore({ IllegalStateException.class, SecurityException.class })
    public void contextDestroyed(ServletContextEvent sce) {
        if (policyFactory != null) {
            PolicyFactory currentFactory = null;
            try {
                currentFactory = PolicyFactory.getPolicyFactory();
            } catch (SecurityException se) {
            }
            if (currentFactory == policyFactory) {
                PolicyFactory wrappedFactory = currentFactory.getWrapped();
                // If the applications provided Factory did not have a constructor that took the previous
                // Factory instance, use the one saved off when the Factory was set.
                if (wrappedFactory == null) {
                    wrappedFactory = previousPolicyFactory;
                }
                PolicyFactory.setPolicyFactory(wrappedFactory);
            }
        }

        if (configFactory != null) {
            PolicyConfigurationFactory currentFactory = null;
            try {
                currentFactory = PolicyConfigurationFactory.get();
            } catch (IllegalStateException ise) {
                // expected if it was removed
            }
            if (currentFactory == configFactory) {
                PolicyConfigurationFactory wrappedFactory = currentFactory.getWrapped();
                // If the applications provided Factory did not have a constructor that took the previous
                // Factory instance, use the one saved off when the Factory was set.
                if (wrappedFactory == null) {
                    wrappedFactory = previousConfigFactory;
                }
                PolicyConfigurationFactory.setPolicyConfigurationFactory(wrappedFactory);
            }
        }
    }
}
