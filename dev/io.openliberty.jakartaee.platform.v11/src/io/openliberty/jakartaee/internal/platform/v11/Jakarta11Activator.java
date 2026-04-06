/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jakartaee.internal.platform.v11;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * Disables Java SecurityManager if it is enabled since Jakarta EE 11 does not support it.
 */
@SuppressWarnings("removal")
public class Jakarta11Activator implements BundleActivator {

    private static final TraceComponent tc = Tr.register(Jakarta11Activator.class, "bootstrap", "com.ibm.ws.kernel.boot.resources.LauncherMessages");

    private SecurityManager securityManager = null;

    @Override
    public void start(BundleContext arg0) throws Exception {
        if (JavaInfo.majorVersion() == 17) {
            securityManager = System.getSecurityManager();
            if (securityManager != null) {
                System.setSecurityManager(null);
                Tr.warning(tc, "warning.java2security.stopped");
            }
        }
    }

    @Override
    public void stop(BundleContext arg0) throws Exception {
        // If stopping EE 11 features to replace them with older EE features,
        // re-enable the SecurityManager and put out an informational message.
        if (securityManager != null && !FrameworkState.isStopping()) {
            System.setSecurityManager(securityManager);
            Tr.info(tc, "info.java2security.restarted");
        }
    }

}
