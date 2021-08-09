/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.connector.internal;

import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedModuleInfo;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.module.internal.ModuleHandlerBase;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ModuleRuntimeContainer;
import com.ibm.ws.jca.metadata.ConnectorModuleMetaData;

@Component(service = ModuleHandler.class,
           property = { "service.vendor=IBM", "type:String=connector" })
public class ConnectorModuleHandlerImpl extends ModuleHandlerBase {
    private static final TraceComponent tc = Tr.register(ConnectorModuleHandlerImpl.class);

    @Reference(target = "(type=connector)")
    protected void setConnectorContainer(ModuleRuntimeContainer connectorContainer) {
        super.setModuleRuntimeContainer(connectorContainer);
    }

    @Override
    public Future<Boolean> deployModule(DeployedModuleInfo deployedMod, DeployedAppInfo deployedApp) {
        long startTime = System.nanoTime();
        try {
            return super.deployModule(deployedMod, deployedApp);
        } finally {
            ConnectorModuleMetaData metadata = (ConnectorModuleMetaData) deployedMod.getModuleInfo().getMetaData();
            if (metadata != null && metadata.isEmbedded()) {
                if (super.getFirstFailure() == null)
                    Tr.audit(tc, "J2CA7001.adapter.install.successful", metadata.getIdentifier(), (System.nanoTime() - startTime) / 1000000000.0);
                else
                    Tr.error(tc, "J2CA7002.adapter.install.failed", metadata.getIdentifier(), super.getFirstFailure().toString());
            }
        }
    }

    @Override
    public boolean undeployModule(DeployedModuleInfo deployedModule) {
        boolean success = super.undeployModule(deployedModule);

        ConnectorModuleMetaData metadata = (ConnectorModuleMetaData) ((ExtendedModuleInfo)deployedModule.getModuleInfo()).getMetaData();
        if (metadata != null && metadata.isEmbedded()) {
            if (success)
                Tr.audit(tc, "J2CA7009.adapter.uninstalled", metadata.getIdentifier());
            else
                Tr.audit(tc, "J2CA7010.adapter.uninstall.failed", metadata.getIdentifier());
        }
        return success;
    }
}
