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
package com.ibm.ws.security.authentication.jaas.modules;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.ibm.ws.security.authentication.internal.JAASService;
import com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule;

/**
 *
 */
public class LoginModuleHelper {
    private static JAASService testJaasService = null;

    /**
     * Get the {@link JAASService} that contains various services we need to interact with.
     *
     * @return The JAASService.
     */
    public static JAASService getJAASService() {
        if (testJaasService != null) {
            return testJaasService;
        }

        BundleContext bc = FrameworkUtil.getBundle(IdentityAssertionLoginModule.class).getBundleContext();
        return bc.getService(bc.getServiceReference(JAASService.class));
    }

    /**
     * FOR UNIT TESTING ONLY!!!!
     *
     * Set the test JAASService.
     *
     * @param service The JAASService to use for unit testing.
     */
    public static void setTestJaasService(JAASService service) {
        testJaasService = service;
    }
}
