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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    // Time stamps of WAR files which have been expanded during this JVM launch..
    
    // TODO: This will cause a slow memory leak during a long run which adds then removes
    //       a long sequence of applications.
    //
    //       But, there will also be a gradual accumulation of files on disk, as removing
    //       unexpanded WAR files doesn't cause the expansions of those files to be removed.


    private static final Map<String, Long> expansionStamps = new HashMap<String, Long>(1);

    /**
     * Tell if a file should be expanded by answering the updated time stamp
     * of the file.
     * 
     * If the file was previously expanded during this JVM run, and the file
     * time stamp is the same as when the file was expanded, answer null.
     * 
     * If either the file was not yet expanded during this JVM run, or if the
     * the current file time stamp is different than the time stamp of the file
     * when it was expanded, update the recorded time stamp, and answer the new
     * time stamp.
     *
     * @param absPath The absolute path of the file which is to be tested.
     * @param file The file which is to be tested.
     *
     * @return The new time stamp of the file, if the file is to be expanded.
     *     Null if the file is not to be expanded.
     */
    private static Long getUpdatedStamp(String absPath, File file) {
        String methodName = "getUpdatedStamp";
        boolean doDebug = tc.isDebugEnabled();

        Long currentStamp = Long.valueOf( file.lastModified() );

        Long newStamp;
        String newStampReason = null;

        synchronized ( expansionStamps ) {
            Long priorStamp = expansionStamps.put(absPath, currentStamp);

            if ( priorStamp == null ) {
                newStamp = currentStamp;
                if ( doDebug ) {
                    newStampReason = "First extraction; stamp [ " + currentStamp + " ]";
                }
            } else if ( currentStamp.longValue() != priorStamp.longValue() ) {
                newStamp = currentStamp;
                if ( doDebug ) {
                    newStampReason = "Additional extraction; old stamp [ " + priorStamp + " ] new stamp [ " + currentStamp + " ]";
                }
            } else {
                newStamp = null;
                if ( doDebug ) {
                    newStampReason = "No extraction; stamp [ " + currentStamp + " ]";
                }
            }
        }

        if ( doDebug ) {
            Tr.debug(tc, methodName + ": " + newStampReason);
        }
        return newStamp;
    }

    private static void clearStamp(String absPath, File file) {
        synchronized ( expansionStamps ) { 
            expansionStamps.remove(absPath);
        }
    }

    protected void expand(
        String name, File collapsedFile,
        WsResource expandedResource, File expandedFile) throws IOException {

        String collapsedPath = collapsedFile.getAbsolutePath();

        Long updatedStamp = getUpdatedStamp(collapsedPath, collapsedFile);
        if ( updatedStamp == null ) {
            // Nothing to do: Already extracted by this JVM, and has the same
            // time stamp as when previously extracted.
            return;
        }

        try {
            if ( expandedFile.exists() ) {
                File failedDelete = ZipUtils.deleteWithRetry(expandedFile);
                if ( failedDelete != null ) {
                    if ( failedDelete == expandedFile ) {
                        throw new IOException("Failed to delete [ " + expandedFile.getAbsolutePath() + " ]");
                    } else {
                        throw new IOException("Failed to delete [ " + expandedFile.getAbsolutePath() + " ] because [ " + failedDelete.getAbsolutePath() + " ] could not be deleted.");
                    }
                }
            }

            expandedResource.create();

            ZipUtils.unzip(collapsedFile, expandedFile, ZipUtils.IS_NOT_EAR, updatedStamp); // throws IOException

        } catch ( IOException e ) {
            // Forget about the expansion if it failed.
            clearStamp(collapsedPath, collapsedFile);

            throw e;
        }
    }

    private boolean isArchive(File file, String path) {
        if ( path.toLowerCase().endsWith(XML_SUFFIX) ) {
            return false;
        } else if ( !file.isFile() ) {
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
