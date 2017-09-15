/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.library.internal.LibraryStatusService;
import com.ibm.wsspi.classloading.ApplicationClassloadingService;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.library.Library;

/**
 * Provides services for applications to load and create classloaders.
 * One instance of this service exists for every application
 * that has a classloader configured in the server.xml.
 */

@Component(configurationPid = ApplicationClassloadingServiceFactory.COMPONENT_FACTORY_PID, configurationPolicy = ConfigurationPolicy.REQUIRE, property = "service.vendor=IBM")
public class ApplicationClassloadingServiceImpl implements ApplicationClassloadingService {

    @SuppressWarnings("unused")
    private volatile Library globalSharedLibrary;
    @SuppressWarnings("unused")
    private volatile ClassLoadingService classLoadingService;
    @SuppressWarnings("unused")
    private volatile ConfigurationAdmin configAdmin;
    @SuppressWarnings("unused")
    private Dictionary<String, Object> config;

    @Reference(name = "libraryStatus", policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, target = "(id=unbound)")
    protected void setLibraryStatus(LibraryStatusService libraryStatus) {}

    protected void unsetLibraryStatus(LibraryStatusService libraryStatus) {}

    @Reference(name = "globalSharedLibrary", policy = ReferencePolicy.STATIC, target = "(id=global)")
    protected void setGlobalSharedLibrary(Library library) {
        this.globalSharedLibrary = library;
    }

    protected void unsetGlobalSharedLibrary(Library library) {}

    @Reference(name = "classLoadingService", policy = ReferencePolicy.STATIC)
    protected void setClassLoadingService(ClassLoadingService classLoadingService) {
        this.classLoadingService = classLoadingService;
    }

    protected void unsetClassLoadingService(ClassLoadingService classLoadingService) {}

    @Reference(name = "configAdmin", policy = ReferencePolicy.STATIC)
    protected void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    protected void unsetConfigAdmin(ConfigurationAdmin configAdmin) {}

    public void update(Dictionary<String, Object> props) {
        config = props;
    }

}
