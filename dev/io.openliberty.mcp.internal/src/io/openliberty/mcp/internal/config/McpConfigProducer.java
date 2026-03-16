/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.config;

import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.mcp.internal.moduleScope.ModuleScoped;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class McpConfigProducer {

    private static final ServiceCaller<McpServerApplicationTracker> trackerService = new ServiceCaller<>(McpConfigProducer.class, McpServerApplicationTracker.class);

    @Produces
    @ModuleScoped
    public McpConfig produceMcpConfig() {
        ComponentMetaData componentMetaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();

        if (componentMetaData == null) {
            return McpServerConfigProps.DEFAULT_CONFIG;
        }

        String appName = componentMetaData.getJ2EEName().getApplication();
        String moduleName = componentMetaData.getJ2EEName().getModule();

        return trackerService.run(tracker -> tracker.getConfigForModule(appName, moduleName))
                             .orElse(McpServerConfigProps.DEFAULT_CONFIG);
    }
}
