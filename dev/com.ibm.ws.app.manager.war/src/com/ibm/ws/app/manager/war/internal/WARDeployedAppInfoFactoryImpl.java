/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.war.internal;

import java.io.File;
import java.io.IOException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.ApplicationManager;
import com.ibm.ws.app.manager.module.AbstractDeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.DeployedAppServices;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.container.service.app.deploy.extended.ApplicationInfoForContainer;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.kernel.service.location.WsResource;

@Component(service = DeployedAppInfoFactory.class,
           property = { "service.vendor=IBM", "type:String=war" })
public class WARDeployedAppInfoFactoryImpl extends AbstractDeployedAppInfoFactory implements DeployedAppInfoFactory {

    private static final TraceComponent tc = Tr.register(WARDeployedAppInfoFactoryImpl.class);

    private static final String DEFAULT_APP_LOCATION = "${server.config.dir}/apps/expanded/";

    @Reference
    protected DeployedAppServices deployedAppServices;
    @Reference(target = "(type=web)")
    protected ModuleHandler webModuleHandler;
    @Reference
    protected ApplicationManager applicationManager;

    // WAR expansion ...
    protected void prepareExpansion(String warName) throws IOException {
        WsResource expansionResource;
        try {
            expansionResource = deployedAppServices.getLocationAdmin().resolveResource(applicationManager.getExpandLocation());
            if (expansionResource != null && expansionResource.isType(WsResource.Type.DIRECTORY)) {
                expansionResource.create();
            } else {
                // if we cant resolve the expandLocation value default it and warn the user
                Tr.warning(tc, "warning.could.not.expand.app.loc", warName, applicationManager.getExpandLocation());
                expansionResource = null;
                expansionResource = deployedAppServices.getLocationAdmin().resolveResource(DEFAULT_APP_LOCATION);
                expansionResource.create();
            }
        } catch (Exception ex) {
            Tr.warning(tc, "warning.could.not.expand.app.loc", warName, applicationManager.getExpandLocation());
            expansionResource = null;
            expansionResource = deployedAppServices.getLocationAdmin().resolveResource(DEFAULT_APP_LOCATION);
            expansionResource.create();
        }
    }

    protected WsResource resolveExpansion(String appName) {
        return deployedAppServices.getLocationAdmin().resolveResource(applicationManager.getExpandLocation() + appName + ".war/");
    }

    protected void expand(
                          String name, File collapsedFile,
                          WsResource expandedResource, File expandedFile) throws IOException {

        String collapsedPath = collapsedFile.getAbsolutePath();

        if (expandedFile.exists()) {
            File failedDelete = ZipUtils.deleteWithRetry(expandedFile);
            if (failedDelete != null) {
                if (failedDelete == expandedFile) {
                    throw new IOException("Failed to delete [ " + expandedFile.getAbsolutePath() + " ]");
                } else {
                    throw new IOException("Failed to delete [ " + expandedFile.getAbsolutePath() + " ] because [ " + failedDelete.getAbsolutePath()
                                          + " ] could not be deleted.");
                }
            }
        }

        expandedResource.create();

        ZipUtils.unzip(collapsedFile, expandedFile, ZipUtils.IS_NOT_EAR, collapsedFile.lastModified()); // throws IOException

    }

    @Override
    public WARDeployedAppInfo createDeployedAppInfo(ApplicationInformation<DeployedAppInfo> appInfo) throws UnableToAdaptException {

        String warPid = appInfo.getPid();
        String warName = appInfo.getName();
        String warPath = appInfo.getLocation();
        File warFile = new File(warPath);

        Tr.debug(tc, "Create deployed application: PID [ " + warPid + " ] Name [ " + warName + " ] Location [ " + warPath + " ]");

        BinaryType appType = getApplicationType(warFile, warPath);
        if (appType == BinaryType.LOOSE) {
            Tr.info(tc, "info.loose.app", warName, warPath);
        } else if (appType == BinaryType.DIRECTORY) {
            Tr.info(tc, "info.directory.app", warName, warPath);
        } else if (applicationManager.getExpandApps()) {

            try {
                prepareExpansion(warName);

                WsResource expandedResource = resolveExpansion(warName);
                File expandedFile = expandedResource.asFile();
                if (applicationManager.shouldExpand(expandedFile.getName(), warFile, expandedFile)) {
                    Tr.info(tc, "info.expanding.app", warName, warPath, expandedFile.getAbsolutePath());
                    expand(warName, warFile, expandedResource, expandedFile);
                }

                // Issue 17268: APAR PH37460 useJandex is ignored when autoExpand is set
                //
                // Transport the application information from the initial application container
                // the expanded application container.
                //
                // See dev/com.ibm.ws.container.service/src/com/ibm/ws/container/service/app/deploy/internal/ApplicationInfoImpl.<init>,
                // which retrieves the 'ApplicationInfoForContainer' from the application container.
                //
                // When this information is available, ApplicationInfoImpl.getUseJandex() delegates it for the use jandex value.
                //
                // Interface 'ApplicationInfoForContainer' is implemented by concrete type
                // 'dev/com.ibm.ws.app.manager/src/com/ibm/ws/app/manager/internal/ApplicationInstallInfo'.  
                //
                // ApplicationInstallInfo.<init> is created using an application container.  The initializer has a step which
                // stores the new instance to the application container's non-persistent cache.                

                // Part 1: Retrieve the container info from the initial container.
                NonPersistentCache initialCache =
                    appInfo.getContainer().adapt(NonPersistentCache.class);
                ApplicationInfoForContainer appContainerInfo = (ApplicationInfoForContainer)
                    initialCache.getFromCache(ApplicationInfoForContainer.class);

                // Tr.info(tc, "Initial 'useJandex' [ " +
                //     ((appContainerInfo == null) ? "unavailable" : appContainerInfo.getUseJandex()) + " ]");

                Container expandedContainer = deployedAppServices.setupContainer(warPid, expandedFile);
                appInfo.setContainer(expandedContainer);
                
                // Part 2: Store the container info on the expanded container.                
                if ( appContainerInfo != null ) {
                    NonPersistentCache finalCache =
                        appInfo.getContainer().adapt(NonPersistentCache.class);
                    finalCache.addToCache(ApplicationInfoForContainer.class, appContainerInfo);
                }

            } catch (IOException e) {
                Tr.error(tc, "warning.could.not.expand.application", warName, e.getMessage());
            }
        } else {
            Tr.info(tc, "info.unexpanded.app", warName, warPath);
        }

        WARDeployedAppInfo deployedApp = new WARDeployedAppInfo(appInfo, deployedAppServices, webModuleHandler);
        appInfo.setHandlerInfo(deployedApp);

        return deployedApp;
    }
}
