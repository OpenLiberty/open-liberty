/*******************************************************************************
 * Copyright (c) 2012,2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.sar;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.internal.ContextRootUtil;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoBase;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.module.internal.ModuleInfoUtils;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.classloading.GatewayConfiguration;


/**
 * @author SAGIA
 * The deplyed app info for sar applications
 */
class SARDeployedAppInfo extends DeployedAppInfoBase {
	   private static final String CONTEXT_ROOT = "context-root";

	    private final ModuleHandler webModuleHandler;
	    private final WebModuleContainerInfo webContainerModuleInfo;

	    SARDeployedAppInfo(ApplicationInformation<DeployedAppInfo> applicationInformation,
	                       SARDeployedAppInfoFactoryImpl factory) throws UnableToAdaptException {
	        super(applicationInformation, factory);
	        this.webModuleHandler = factory.webModuleHandler;


	        String moduleURI = ModuleInfoUtils.getModuleURIFromLocation(applicationInformation.getLocation());
	        String contextRoot = ContextRootUtil.getContextRoot((String) applicationInformation.getConfigProperty(CONTEXT_ROOT));
	        //tWAS doesn't use the ibm-web-ext to obtain the context-root when the WAR exists in an EAR.
	        //this call is only valid for WAR-only
	        if (contextRoot == null) {
	            contextRoot = ContextRootUtil.getContextRoot(getContainer());
	        }
	        this.webContainerModuleInfo = new WebModuleContainerInfo(webModuleHandler,
                            factory.getModuleMetaDataExtenders().get("web"),
                            factory.getNestedModuleMetaDataFactories().get("web"),
	                          applicationInformation.getContainer(), null,
	                          moduleURI, moduleClassesInfo, contextRoot);
	        moduleContainerInfos.add(webContainerModuleInfo);
	    }

	    /**
	     * Specify the packages to be imported dynamically into all web apps
	     */
	    private static final List<String> DYNAMIC_IMPORT_PACKAGE_LIST = Collections.unmodifiableList(Arrays.asList("*"));

	    @Override
	    public ClassLoader createModuleClassLoader(ModuleInfo moduleInfo, List<ContainerInfo> moduleClassesContainers) {
	        if (moduleInfo instanceof WebModuleInfo) {
	            ApplicationInfo appInfo = moduleInfo.getApplicationInfo();
	            String j2eeAppName = appInfo.getDeploymentName();
	            String j2eeModuleName = moduleInfo.getURI();
	            ClassLoadingService cls = classLoadingService;
	            List<Container> containers = new ArrayList<Container>();
	            for (ContainerInfo containerInfo : moduleClassesContainers) {
	                containers.add(containerInfo.getContainer());
	            }

	            GatewayConfiguration gwCfg = cls.createGatewayConfiguration()
	                            // TODO call .setApplicationVersion() with some appropriate value
	                            .setApplicationName(j2eeAppName)
	                            .setDynamicImportPackage(DYNAMIC_IMPORT_PACKAGE_LIST);

	            ProtectionDomain protectionDomain = getProtectionDomain();
	            
	            ClassLoaderConfiguration clCfg = cls.createClassLoaderConfiguration()
	                            .setId(cls.createIdentity("WebModule", j2eeAppName + "#" + j2eeModuleName))
	            				.setProtectionDomain(protectionDomain);
	            
	            return createTopLevelClassLoader(containers, gwCfg, clCfg);
	        } else {
	            return null;
	        }
	    }

	    @Override
	    protected ExtendedApplicationInfo createApplicationInfo() {
	        ExtendedApplicationInfo appInfo = appInfoFactory.createApplicationInfo(getName(),
	                                                                       webContainerModuleInfo.moduleName,
	                                                                       getContainer(),
	                                                                       this,
	                                                                       getConfigHelper());
	        webContainerModuleInfo.moduleName = appInfo.getName();
	        // ??? Contrary to the EE specs, we use the deployment name, not the EE
	        // application name, as the default context root for compatibility.
	        webContainerModuleInfo.defaultContextRoot = getName();
	        return appInfo;
	    }

	    @Override
	    public List<ModuleClassesContainerInfo> getModuleClassesContainerInfo() {
	        return Collections.singletonList((ModuleClassesContainerInfo) webContainerModuleInfo);
	    }
}
