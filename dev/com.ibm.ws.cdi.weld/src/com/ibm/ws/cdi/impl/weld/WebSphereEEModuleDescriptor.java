/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
package com.ibm.ws.cdi.impl.weld;

import org.jboss.weld.bootstrap.spi.helpers.EEModuleDescriptorImpl;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.cdi.internal.interfaces.ArchiveType;

/**
 *
 */
public class WebSphereEEModuleDescriptor extends EEModuleDescriptorImpl {

    private final J2EEName j2eeName;

    /**
     * @param id
     * @param archiveType
     */
    public WebSphereEEModuleDescriptor(String id, ArchiveType archiveType) {
        super(id, getModuleType(archiveType));
        j2eeName = null;
    }

    public WebSphereEEModuleDescriptor(String id, J2EEName j2eeName, ArchiveType archiveType) {
        super(id, getModuleType(archiveType));
        this.j2eeName = j2eeName;
    }

    private static ModuleType getModuleType(ArchiveType archiveType) {
        ModuleType moduleType = ModuleType.EAR;
        switch (archiveType) {
            case EAR_LIB:
                moduleType = ModuleType.EAR;
                break;
            case MANIFEST_CLASSPATH:
                moduleType = ModuleType.WEB;
                break;
            case WEB_INF_LIB:
                moduleType = ModuleType.WEB;
                break;
            case WEB_MODULE:
                moduleType = ModuleType.WEB;
                break;
            case EJB_MODULE:
                moduleType = ModuleType.EJB_JAR;
                break;
            case CLIENT_MODULE:
                moduleType = ModuleType.APPLICATION_CLIENT;
                break;
            case RAR_MODULE:
                moduleType = ModuleType.CONNECTOR;
                break;
            case SHARED_LIB:
                moduleType = ModuleType.EAR;
                break;
            case ON_DEMAND_LIB:
                moduleType = ModuleType.EAR;
                break;
            case RUNTIME_EXTENSION:
                moduleType = ModuleType.EAR;
                break;
            default:
                moduleType = ModuleType.EAR;
                break;
        }
        return moduleType;
    }

    public J2EEName getJ2eeName() {
        return j2eeName;
    }
}
