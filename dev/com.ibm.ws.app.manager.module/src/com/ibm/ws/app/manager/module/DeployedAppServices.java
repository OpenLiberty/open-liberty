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
     * @return
     */
    ApplicationInfoFactory getApplicationInfoFactory();

    /**
     * @return
     */
    MetaDataService getMetaDataService();

    /**
     * @return
     */
    StateChangeService getStateChangeService();

    /**
     * @return
     */
    ClassLoadingService getClassLoadingService();

    /**
     * @return
     */
    Library getGlobalSharedLibrary();

    /**
     * @return
     */
    String getGlobalSharedLibraryPid();

    /**
     * @return
     */
    FutureMonitor getFutureMonitor();

    /**
     * @return
     */
    ConfigurationAdmin getConfigurationAdmin();

    /**
     * @return
     */
    PermissionManager getPermissionManager();

    /**
     * @return
     */
    List<ModuleMetaDataExtender> getModuleMetaDataExtenders(String moduleType);

    /**
     * @return
     */
    List<NestedModuleMetaDataFactory> getNestedModuleMetaDataFactories(String moduleType);

    /**
     * @return
     */
    WsLocationAdmin getLocationAdmin();

    /**
     * @return
     */
    ArtifactContainerFactory getArtifactFactory();

    /**
     * @return
     */
    AdaptableModuleFactory getModuleFactory();

    /**
     * @param pid
     * @return
     */
    List<Library> getLibrariesFromPid(String pid) throws InvalidSyntaxException;

    /**
     * @param pid
     * @param asFile
     * @return
     */
    Container setupContainer(String pid, File file);

}
