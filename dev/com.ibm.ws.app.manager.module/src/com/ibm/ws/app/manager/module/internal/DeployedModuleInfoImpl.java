/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.module.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedModuleInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.threading.FutureMonitor;

public final class DeployedModuleInfoImpl implements DeployedModuleInfo {
    private final ModuleHandler moduleHandler;
    private final ExtendedModuleInfo moduleInfo;
    private boolean starting = false;
    private boolean started = false;
    private List<DeployedModuleInfoImpl> nestedModules = null;

    public DeployedModuleInfoImpl(ModuleHandler moduleHandler, ExtendedModuleInfo moduleInfo) {
        this.moduleHandler = moduleHandler;
        this.moduleInfo = moduleInfo;
    }

    void addNestedModule(DeployedModuleInfoImpl nestedDeployedMod) {
        if (nestedModules == null) {
            nestedModules = new ArrayList<DeployedModuleInfoImpl>(2);
        }
        nestedModules.add(nestedDeployedMod);
    }

    List<DeployedModuleInfoImpl> getNestedModules() {
        return nestedModules;
    }

    Future<Boolean> installModule(DeployedAppInfo deployedApp, FutureMonitor futureMonitor, ContainerInfo.Type moduleContainerType) {
        if (this.moduleHandler != null) {
            if (this.moduleInfo != null) {
                return this.moduleHandler.deployModule(this, deployedApp);
            } else {
                if (futureMonitor != null) {
                    return futureMonitor.createFutureWithResult(Boolean.class, new IllegalStateException(moduleContainerType.toString()));
                }
            }
        } else {
            if (futureMonitor != null) {
                return futureMonitor.createFutureWithResult(true);
            }
        }
        return null;
    }

    boolean uninstallModule() {
        return this.moduleHandler.undeployModule(this);
    }

    @Override
    public ExtendedModuleInfo getModuleInfo() {
        return this.moduleInfo;
    }

    @Override
    public void setIsStarting() {
        starting = true;
    }

    @Override
    public void setIsStarted() {
        started = true;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isStarting() {
        return starting;
    }
}
