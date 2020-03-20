/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.ApplicationManager;
import com.ibm.ws.app.manager.internal.AppManagerConstants;
import com.ibm.ws.app.manager.module.AbstractDeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.DeployedAppServices;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.kernel.service.location.WsResource;

@Component(service = DeployedAppInfoFactory.class,
           property = { "service.vendor=IBM", "type:String=war" })
public class WARDeployedAppInfoFactoryImpl extends AbstractDeployedAppInfoFactory implements DeployedAppInfoFactory {

    private static final TraceComponent tc = Tr.register(WARDeployedAppInfoFactoryImpl.class);

    @Reference
    protected DeployedAppServices deployedAppServices;
    @Reference(target = "(type=web)")
    protected ModuleHandler webModuleHandler;
    @Reference
    protected ApplicationManager applicationManager;

    // WAR expansion ...

    protected void prepareExpansion() throws IOException {
        WsResource expansionResource = deployedAppServices.getLocationAdmin().resolveResource(AppManagerConstants.EXPANDED_APPS_DIR);
        expansionResource.create();
    }

    protected WsResource resolveExpansion(String appName) {
        return deployedAppServices.getLocationAdmin().resolveResource(AppManagerConstants.EXPANDED_APPS_DIR + appName + ".war/");
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
                prepareExpansion();

                WsResource expandedResource = resolveExpansion(warName);
                File expandedFile = expandedResource.asFile();
                if (applicationManager.shouldExpand(expandedFile.getName(), warFile, expandedFile)) {
                    Tr.info(tc, "info.expanding.app", warName, warPath, expandedFile.getAbsolutePath());
                    expand(warName, warFile, expandedResource, expandedFile);
                }

                Container expandedContainer = deployedAppServices.setupContainer(warPid, expandedFile);
                appInfo.setContainer(expandedContainer);

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
