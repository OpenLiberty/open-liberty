/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.ws.security.authentication.internal.JAASService;

/**
 *
 */
public class LoginModuleHelper {
    private static JAASService testJaasService = null;
    private static final ServiceCaller<JAASService> jaasService = new ServiceCaller<>(LoginModuleHelper.class, JAASService.class);

    /**
     * Get the {@link JAASService} that contains various services we need to interact with.
     *
     * @return The JAASService.
     */
    public static JAASService getJAASService() {
        if (testJaasService != null) {
            return testJaasService;
        }

        return jaasService.current().orElseThrow(() -> new NullPointerException("No JAASService found."));
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
