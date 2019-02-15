/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
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

    // Record of what WAR files have been expanded this time the server
    // was started.
    //
    // The preference is to remove this record and rely entirely on
    // time stamps.  Because of test implications, that change has not
    // yet been made.

    private static final Set<String> expanded = new HashSet<String>(1);

    private static boolean didExpand(String path) {
    	synchronized ( expanded ) {
    		return ( expanded.contains(path) );
    	}
    }
    private static void recordExpansion(String path) {
    	synchronized ( expanded ) {
    		expanded.add(path);
    	}
    }

    private static void clearExpansion(String path) {
    	synchronized ( expanded ) { 
    		expanded.remove(path);
    	}
    }

    private long getStamp(File file) {
        return file.lastModified();
    }

    protected void expand(
        String name, File collapsedFile,
        WsResource expandedResource, File expandedFile) throws IOException {

        String expandedPath = expandedFile.getAbsolutePath();

        long newStamp = getStamp(collapsedFile);

        boolean doDelete;
        boolean doExpand;

        if ( expandedResource.exists() ) {
        	// 'didExpand' is used to preserve the prior behavior.
        	// Remove this logic to enable re-use of expansions across
        	// restarts.  The logic has not been removed because there
        	// are test implications.

        	if ( didExpand(expandedPath) ) {
        		long oldStamp = getStamp(expandedFile);
        		if ( oldStamp != newStamp ) {
        			if ( tc.isDebugEnabled() ) {
        				Tr.debug(tc, "Same Launch: Delete and re-extract [ " + name + " ] from [ " + collapsedFile.getPath() + " ] to [ " + expandedPath + " ]");
        			}
        			doDelete = true;
        			doExpand = true;
        		} else {
        			if ( tc.isDebugEnabled() ) {
        				Tr.debug(tc, "Same Launch: Reuse extraction of [ " + name + " ] from [ " + collapsedFile.getPath() + " ] to [ " + expandedPath + " ]");
        			}
        			doDelete = false;
        			doExpand = false;
        		}
        	} else {
    			if ( tc.isDebugEnabled() ) {
    				Tr.debug(tc, "New Launch: Delete and re-extract [ " + name + " ] from [ " + collapsedFile.getPath() + " ] to [ " + expandedPath + " ]");
    			}
        		doDelete = true;
        		doExpand = true;
        	}
        	
        } else {
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, "Extract [ " + name + " ] from [ " + collapsedFile.getPath() + " ] to [ " + expandedPath + " ]");
            }
            doDelete = false;
            doExpand = true;
        }

        if ( doDelete || doExpand ) {
        	ZipUtils zipUtils = new ZipUtils();

        	// The order (clear, delete, unzip, record) is deliberate:
        	// The expansion should be recorded only if successful.

            if ( doDelete ) {
            	clearExpansion(expandedPath);

                File failedDelete = zipUtils.deleteWithRetry(expandedFile);
                if ( failedDelete != null ) {
                    if ( failedDelete == expandedFile ) {
                    	throw new IOException("Failed to delete [ " + expandedPath + " ]");
                    } else {
                    	throw new IOException("Failed to delete [ " + expandedPath + " ] because [ " + failedDelete.getAbsolutePath() + " ] could not be deleted.");
                    }
                }
            }

            if ( doExpand ) {
                expandedResource.create();
                zipUtils.unzip(collapsedFile, expandedFile, ZipUtils.IS_NOT_EAR, newStamp); // throws IOException
                recordExpansion(expandedPath);
            }
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
