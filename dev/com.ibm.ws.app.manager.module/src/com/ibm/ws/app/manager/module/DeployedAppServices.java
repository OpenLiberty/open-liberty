/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.module;

import java.io.File;
import java.util.List;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.ws.classloading.java2sec.PermissionManager;
import com.ibm.ws.container.service.app.deploy.extended.ApplicationInfoFactory;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.container.service.metadata.extended.ModuleMetaDataExtender;
import com.ibm.ws.container.service.metadata.extended.NestedModuleMetaDataFactory;
import com.ibm.ws.container.service.state.StateChangeService;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.library.Library;

/**
 *
 */
public interface DeployedAppServices {

    /**
     * @return from services injected by DS
     */
    ApplicationInfoFactory getApplicationInfoFactory();

    /**
     * @return from services injected by DS
     */
    MetaDataService getMetaDataService();

    /**
     * @return from services injected by DS
     */
    StateChangeService getStateChangeService();

    /**
     * @return from services injected by DS
     */
    ClassLoadingService getClassLoadingService();

    /**
     * @return from services injected by DS
     */
    Library getGlobalSharedLibrary();

    /**
     * @return from services injected by DS
     */
    String getGlobalSharedLibraryPid();

    /**
     * @return from services injected by DS
     */
    FutureMonitor getFutureMonitor();

    /**
     * @return from services injected by DS
     */
    ConfigurationAdmin getConfigurationAdmin();

    /**
     * @return from services injected by DS
     */
    PermissionManager getPermissionManager();

    /**
     * @return from services injected by DS
     */
    List<ModuleMetaDataExtender> getModuleMetaDataExtenders(String moduleType);

    /**
     * @return from services injected by DS
     */
    List<NestedModuleMetaDataFactory> getNestedModuleMetaDataFactories(String moduleType);

    /**
     * @return from services injected by DS
     */
    WsLocationAdmin getLocationAdmin();

    /**
     * @return from services injected by DS
     */
    ArtifactContainerFactory getArtifactFactory();

    /**
     * @return from services injected by DS
     */
    AdaptableModuleFactory getModuleFactory();

    /**
     * @param pid
     * @return from services injected by DS
     */
    List<Library> getLibrariesFromPid(String pid) throws InvalidSyntaxException;

    /**
     * @param pid
     * @param file
     * @return from services injected by DS
     */
    Container setupContainer(String pid, File file);

}
