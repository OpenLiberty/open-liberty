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
package com.ibm.ws.cdi.internal.interfaces;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.enterprise.inject.spi.Extension;
import javax.servlet.ServletContext;

import org.jboss.weld.bootstrap.spi.Metadata;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Provides methods which allow integration with the Weld Development Mode.
 * Development Mode is only supported between Weld 2.4.x and Weld 4.x (CDI verisons 1.2, 2.0 and 3.0).
 * Weld 5.1 dropped support for development mode (CDI 4.0).
 */
public interface WeldDevelopmentMode {

    public static final TraceComponent tc = Tr.register(WeldDevelopmentMode.class);

    //The System Property which enables Weld Development Mode
    public final static String DEVELOPMENT_MODE = "org.jboss.weld.development";

    public static final boolean developmentMode = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
        @Override
        public Boolean run() {
            String developmentModeStr = System.getProperty(DEVELOPMENT_MODE);
            Boolean developmentMode = Boolean.valueOf(developmentModeStr);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "WeldDevelopmentMode",
                         "The system property " + DEVELOPMENT_MODE + " : " + developmentMode);
            }
            return developmentMode;
        }
    });

    /**
     * @return true if Weld Development Mode is enabled
     */
    public default boolean enabled() {
        return developmentMode;
    }

    /**
     * Create an ExtensionArchive containing only the Weld ProbeExtension class
     *
     * @return an ExtensionArchive instance
     */
    public ExtensionArchive getProbeExtensionArchive(CDIRuntime cdiRuntime);

    /**
     * Load the ProbeExtension class and create an instance.
     *
     * @return A Metadata object containing the loaded ProbeExtension class and an instance of the ProbeExtension
     */
    public Metadata<Extension> getProbeExtension();

    /**
     * Find the WebSphereBeanDeploymentArchive which contains the ProbeExtension
     *
     * @param deployment The deployment to look in
     * @return a WebSphereBeanDeploymentArchive containing the ProbeExtension
     */
    public WebSphereBeanDeploymentArchive getProbeBDA(WebSphereCDIDeployment deployment);

    /**
     * Add the Weld Probe Filter to the ServletContext
     */
    public void addProbeFilter(ServletContext ctx);

}
