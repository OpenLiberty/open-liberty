/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.ejb.internal;

import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoBase;
import com.ibm.ws.app.manager.module.internal.ModuleInfoUtils;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.EJBModuleInfo;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.classloading.GatewayConfiguration;

class EJBDeployedAppInfo extends DeployedAppInfoBase {

    private final EJBModuleContainerInfo ejbContainerModuleInfo;

    EJBDeployedAppInfo(ApplicationInformation<DeployedAppInfo> applicationInformation,
                       EJBDeployedAppInfoFactoryImpl factory) throws UnableToAdaptException {
        super(applicationInformation, factory);

        String moduleURI = ModuleInfoUtils.getModuleURIFromLocation(applicationInformation.getLocation());
        ejbContainerModuleInfo = new EJBModuleContainerInfo(factory.ejbModuleHandler,
                        factory.getModuleMetaDataExtenders().get("ejb"),
                        factory.getNestedModuleMetaDataFactories().get("ejb"),
                        applicationInformation.getContainer(), null,
                        moduleURI, moduleClassesInfo);
        moduleContainerInfos.add(ejbContainerModuleInfo);
    }

    /**
     * Specify the packages to be imported dynamically into all ejb apps
     */
    private static final List<String> DYNAMIC_IMPORT_PACKAGE_LIST = Collections.unmodifiableList(Arrays.asList("*"));

    @Override
    public ClassLoader createModuleClassLoader(ModuleInfo moduleInfo, List<ContainerInfo> moduleClassesContainers) {

        if (moduleInfo instanceof EJBModuleInfo) {
            ApplicationInfo appInfo = moduleInfo.getApplicationInfo();
            Container moduleContainer = moduleInfo.getContainer();
            String j2eeAppName = appInfo.getDeploymentName();
            String j2eeModuleName = moduleInfo.getURI();

            ClassLoadingService cls = classLoadingService;

            List<Container> containers = Collections.singletonList(moduleContainer);

            GatewayConfiguration gwCfg = cls.createGatewayConfiguration()
                            // TODO call .setApplicationVersion() with some appropriate value
                            .setApplicationName(j2eeAppName)
                            .setDynamicImportPackage(DYNAMIC_IMPORT_PACKAGE_LIST);

            ProtectionDomain protectionDomain = getProtectionDomain();

            ClassLoaderConfiguration clCfg = cls.createClassLoaderConfiguration()
                            .setId(cls.createIdentity("EJBModule", j2eeAppName + "#" + j2eeModuleName))
                            .setProtectionDomain(protectionDomain);

            return createTopLevelClassLoader(containers, gwCfg, clCfg);
        }
        else {
            return null;
        }
    }

    @Override
    protected ExtendedApplicationInfo createApplicationInfo() {
        ExtendedApplicationInfo appInfo = appInfoFactory.createApplicationInfo(getName(),
                                                                               ejbContainerModuleInfo.moduleName,
                                                                               getContainer(),
                                                                               this,
                                                                               getConfigHelper());
        ejbContainerModuleInfo.moduleName = appInfo.getName();
        return appInfo;
    }

    @Override
    public List<ModuleClassesContainerInfo> getModuleClassesContainerInfo() {
        return Collections.singletonList((ModuleClassesContainerInfo) ejbContainerModuleInfo);
    }
}
