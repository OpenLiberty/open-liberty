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
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoFactoryBase;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.kernel.service.location.WsResource;

@Component(service = DeployedAppInfoFactory.class,
           property = { "service.vendor=IBM", "type:String=war" })
public class WARDeployedAppInfoFactoryImpl extends DeployedAppInfoFactoryBase {

    private static final TraceComponent tc = Tr.register(WARDeployedAppInfoFactoryImpl.class);

    protected ModuleHandler webModuleHandler;
    private ApplicationManager applicationManager;
    private final ZipUtils zipUtils = new ZipUtils();

    @Reference(target = "(type=web)")
    protected void setWebModuleHandler(ModuleHandler handler) {
        webModuleHandler = handler;
    }

    protected void unsetWebModuleHandler(ModuleHandler handler) {
        webModuleHandler = null;
    }

    @Reference
    protected void setApplicationManager(ApplicationManager mgr) {
        this.applicationManager = mgr;
    }

    protected void unsetApplicationManager(ApplicationManager mgr) {
        this.applicationManager = null;
    }

    // WAR expansion ...

    protected void prepareExpansion() throws IOException {
        WsResource expansionResource = getLocationAdmin().resolveResource(EXPANDED_APPS_DIR);
        expansionResource.create();
    }

    protected WsResource resolveExpansion(String appName) {
        return getLocationAdmin().resolveResource(EXPANDED_APPS_DIR + appName + ".war/");
    }

    private long getStamp(File file) {
        return file.lastModified();
    }

    private void setStamp(File file, long lastModified) {
        file.setLastModified(lastModified);
    }

    protected void expand(
        String warName, File warFile,
        WsResource expandedResource, File expandedFile) throws IOException {

        long newStamp = getStamp(warFile);

        boolean doDelete;
        boolean doExpand;

        if ( expandedResource.exists() ) {
            long oldStamp = getStamp(expandedFile);

            if ( oldStamp != newStamp ) {
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, "Delete and re-extract web module [ " + warName + " ] from [ " + warFile.getPath() + " ] to [ " + expandedFile.getPath() + " ]");
                }
                doDelete = true;
                doExpand = true;
            } else {
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, "Reuse web module [ " + warName + " ] extracted from [ " + warFile.getPath() + " ] to [ " + expandedFile.getPath() + " ]");
                }
                doDelete = false;
                doExpand = false;
            }
        } else {
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, "Extract web module [ " + warName + " ] from [ " + warFile.getPath() + " ] to [ " + expandedFile.getPath() + " ]");
            }
            doDelete = false;
            doExpand = true;
        }

        if ( doDelete || doExpand ) {
        	ZipUtils zipUtils = new ZipUtils();

            if ( doDelete ) {
                zipUtils.deleteWithRetry(expandedFile);
            }

            if ( doExpand ) {
                expandedResource.create();
                zipUtils.unzip(warFile, expandedFile, ZipUtils.IS_NOT_EAR, newStamp);
            }
        }
    }

    private boolean isArchive(File warFile, String warPath) {
        if ( warPath.toLowerCase().endsWith(XML_SUFFIX) ) {
            return false;
        } else if ( !warFile.isFile() ) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public WARDeployedAppInfo createDeployedAppInfo(ApplicationInformation<DeployedAppInfo> appInfo)
        throws UnableToAdaptException {

        String warPid = appInfo.getPid();
        String warName = appInfo.getName();
        String warPath = appInfo.getLocation();
        File warFile = new File(warPath);

        Tr.debug(tc, "Create deployed application: PID [ " + warPid + " ] Name [ " + warName + " ] Location [ " + warPath + " ]");

        if ( applicationManager.getExpandApps() && isArchive(warFile, warPath) ) {
            try {
                prepareExpansion();

                WsResource expandedResource = resolveExpansion(warName);
                File expandedFile = expandedResource.asFile();
                expand(warName, warFile, expandedResource, expandedFile);

                Container expandedContainer = setupContainer(warPid, expandedFile);
                appInfo.setContainer(expandedContainer);

            } catch ( IOException e ) {
                Tr.error(tc, "warning.could.not.expand.application", warName, e.getMessage());
            }
        }

        WARDeployedAppInfo deployedApp = new WARDeployedAppInfo(appInfo, this);
        appInfo.setHandlerInfo(deployedApp);

        return deployedApp;
    }

}
