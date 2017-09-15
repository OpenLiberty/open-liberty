/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.module.internal;

import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ModuleRuntimeContainer;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.container.service.state.StateChangeService;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threading.FutureMonitor;

/**
 *
 */
public abstract class ModuleHandlerBase implements ModuleHandler {
    private FutureMonitor futureMonitor;
    private MetaDataService metaDataService;
    private StateChangeService stateChangeService;
    private ModuleRuntimeContainer moduleRuntimeContainer;
    private Throwable firstFailure;

    @Reference
    protected void setFutureMonitor(FutureMonitor fm) {
        futureMonitor = fm;
    }

    @Reference
    protected void setMetaDataService(MetaDataService metaDataService) {
        this.metaDataService = metaDataService;
    }

    @Reference
    protected void setStateChangeService(StateChangeService stateChangeService) {
        this.stateChangeService = stateChangeService;
    }

    protected ModuleRuntimeContainer getModuleRuntimeContainer() {
        return this.moduleRuntimeContainer;
    }

    protected void setModuleRuntimeContainer(ModuleRuntimeContainer moduleRuntimeContainer) {
        this.moduleRuntimeContainer = moduleRuntimeContainer;
    }

    @Override
    public ModuleMetaData createModuleMetaData(ExtendedModuleInfo moduleInfo, DeployedAppInfo deployedApp) throws MetaDataException {
        ModuleMetaData mmd = moduleRuntimeContainer.createModuleMetaData(moduleInfo);
        deployedApp.moduleMetaDataCreated(moduleInfo, this, mmd);
        return mmd;
    }

    @Override
    public Future<Boolean> deployModule(DeployedModuleInfo deployedMod, DeployedAppInfo deployedApp) {
        this.firstFailure = null;
        ExtendedModuleInfo moduleInfo = deployedMod.getModuleInfo();
        ModuleMetaData mmd = moduleInfo.getMetaData();
        if (mmd == null) {
            deployedApp.uninstallApp();
            return futureMonitor.createFutureWithResult(false);
        }
        try {
            metaDataService.fireModuleMetaDataCreated(mmd, moduleInfo.getContainer());
            for (ModuleMetaData nestedMMD : moduleInfo.getNestedMetaData()) {
                metaDataService.fireModuleMetaDataCreated(nestedMMD, moduleInfo.getContainer());
            }
        } catch (Throwable ex) {
            this.firstFailure = ex;
            deployedApp.uninstallApp();
            return futureMonitor.createFutureWithResult(Boolean.class, ex);
        }

        deployedMod.setIsStarting();
        try {
            stateChangeService.fireModuleStarting(moduleInfo);
        } catch (Throwable ex) {
            this.firstFailure = ex;
            deployedApp.uninstallApp();
            return futureMonitor.createFutureWithResult(Boolean.class, ex);
        }

        Future<Boolean> started;
        try {
            started = moduleRuntimeContainer.startModule(moduleInfo);
        } catch (Throwable ex) {
            this.firstFailure = ex;
            deployedApp.uninstallApp();
            return futureMonitor.createFutureWithResult(Boolean.class, ex);
        }

        deployedMod.setIsStarted();
        try {
            stateChangeService.fireModuleStarted(moduleInfo);
        } catch (Throwable ex) {
            this.firstFailure = ex;
            deployedApp.uninstallApp();
            return futureMonitor.createFutureWithResult(Boolean.class, ex);
        }

        return started;
    }

    @Override
    public boolean undeployModule(DeployedModuleInfo deployedModule) {
        this.firstFailure = null;
        ExtendedModuleInfo extendedModuleInfo = deployedModule.getModuleInfo();
        if (deployedModule.isStarted()) {
            try {
                stateChangeService.fireModuleStopping(extendedModuleInfo);
            } catch (Throwable t) {
                FFDCFilter.processException(t, getClass().getName(), "fireModuleStopping");
                if (this.firstFailure == null) {
                    this.firstFailure = t;
                }
            }

            try {
                moduleRuntimeContainer.stopModule(extendedModuleInfo);
            } catch (Throwable t) {
                FFDCFilter.processException(t, getClass().getName(), "stopModule");
                if (this.firstFailure == null) {
                    this.firstFailure = t;
                }
            }
        }
        if (deployedModule.isStarting()) {
            try {
                stateChangeService.fireModuleStopped(extendedModuleInfo);
            } catch (Throwable t) {
                FFDCFilter.processException(t, getClass().getName(), "fireModuleStopped");
                if (this.firstFailure == null) {
                    this.firstFailure = t;
                }
            }
        }
        try {
            metaDataService.fireModuleMetaDataDestroyed(extendedModuleInfo.getMetaData());
            for (ModuleMetaData nestedMMD : extendedModuleInfo.getNestedMetaData()) {
                metaDataService.fireModuleMetaDataDestroyed(nestedMMD);
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, getClass().getName(), "fireModuleMetaDataDestroyed");
            if (this.firstFailure == null) {
                this.firstFailure = t;
            }
        }
        return this.firstFailure == null;
    }

    protected Throwable getFirstFailure() {
        return this.firstFailure;
    }
}
