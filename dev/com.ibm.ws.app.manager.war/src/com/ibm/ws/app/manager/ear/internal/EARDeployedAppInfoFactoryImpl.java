/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.ear.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.app.manager.ApplicationManager;
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.DeployedAppMBeanRuntime;
import com.ibm.ws.app.manager.module.internal.DeployedAppInfoFactoryBase;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.war.internal.ZipUtils;
import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.app.Module;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.InterpretedContainer;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.kernel.service.location.WsResource;

@Component(service = DeployedAppInfoFactory.class,
           property = { "service.vendor=IBM", "type:String=ear" })
public class EARDeployedAppInfoFactoryImpl extends DeployedAppInfoFactoryBase {
    private static final TraceComponent _tc = Tr.register(EARDeployedAppInfoFactoryImpl.class);

    protected ModuleHandler webModuleHandler;
    protected ModuleHandler ejbModuleHandler;
    protected ModuleHandler clientModuleHandler;
    protected ModuleHandler connectorModuleHandler;
    protected DeployedAppMBeanRuntime appMBeanRuntime;

    private ServiceReference<JavaEEVersion> versionRef;
    protected volatile Version platformVersion = JavaEEVersion.DEFAULT_VERSION;

    private ApplicationManager applicationManager;

    @Reference(service = JavaEEVersion.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected synchronized void setVersion(ServiceReference<JavaEEVersion> reference) {
        versionRef = reference;
        platformVersion = Version.parseVersion((String) reference.getProperty("version"));
    }

    protected synchronized void unsetVersion(ServiceReference<JavaEEVersion> reference) {
        if (reference == this.versionRef) {
            versionRef = null;
            platformVersion = JavaEEVersion.DEFAULT_VERSION;
        }
    }

    @Reference(target = "(type=web)",
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setWebModuleHandler(ModuleHandler handler) {
        webModuleHandler = handler;
    }

    protected void unsetWebModuleHandler(ModuleHandler handler) {
        webModuleHandler = null;
    }

    @Reference(target = "(type=ejb)",
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setEjbModuleHandler(ModuleHandler handler) {
        ejbModuleHandler = handler;
    }

    protected void unsetEjbModuleHandler(ModuleHandler handler) {
        ejbModuleHandler = null;
    }

    @Reference(target = "(type=client)",
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setClientModuleHandler(ModuleHandler handler) {
        clientModuleHandler = handler;
    }

    protected void unsetClientModuleHandler(ModuleHandler handler) {
        clientModuleHandler = null;
    }

    @Reference(target = "(type=connector)",
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setConnectorModuleHandler(ModuleHandler handler) {
        connectorModuleHandler = handler;
    }

    protected void unsetConnectorModuleHandler(ModuleHandler handler) {
        connectorModuleHandler = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setDeployedAppMBeanRuntime(DeployedAppMBeanRuntime appMBeanRuntime) {
        this.appMBeanRuntime = appMBeanRuntime;
    }

    protected void unsetDeployedAppMBeanRuntime(DeployedAppMBeanRuntime appMBeanRuntime) {
        this.appMBeanRuntime = null;
    }

    @Reference
    protected void setApplicationManager(ApplicationManager mgr) {
        this.applicationManager = mgr;
    }

    protected void unsetApplicationManager(ApplicationManager mgr) {
        this.applicationManager = null;
    }

    // Application expansion ...

    protected void prepareExpansion() throws IOException {
        WsResource expansionResource = getLocationAdmin().resolveResource(EXPANDED_APPS_DIR);
        expansionResource.create();
    }

    protected WsResource resolveExpansion(String appName) {
        return getLocationAdmin().resolveResource(EXPANDED_APPS_DIR + appName + ".ear/");
    }

    private long getStamp(File file) {
        return file.lastModified();
    }

    private void setStamp(File file, long lastModified) {
        file.setLastModified(lastModified);
    }

    protected void expand(
        String appName, File appFile,
        WsResource expandedResource, File expandedFile) throws IOException {

        long newStamp = getStamp(appFile);

        boolean doDelete;
        boolean doExpand;

        if ( expandedResource.exists() ) {
            long oldStamp = getStamp(expandedFile);
            if ( oldStamp != newStamp ) {
                if ( _tc.isDebugEnabled() ) {
                    Tr.debug(_tc, "Delete and re-extract application [ " + appName + " ] from [ " + appFile.getPath() + " ] to [ " + expandedFile.getPath() + " ]");
                }
                doDelete = true;
                doExpand = true;
            } else {
                if ( _tc.isDebugEnabled() ) {
                    Tr.debug(_tc, "Reuse application [ " + appName + " ] extracted from [ " + appFile.getPath() + " ] to [ " + expandedFile.getPath() + " ]");
                }
                doDelete = false;
                doExpand = false;
            }
        } else {
            if ( _tc.isDebugEnabled() ) {
                Tr.debug(_tc, "Extract application [ " + appName + " ] from [ " + appFile.getPath() + " ] to [ " + expandedFile.getPath() + " ]");
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
                zipUtils.unzip(appFile, expandedFile, ZipUtils.IS_EAR, newStamp);
            }
        }
    }

    private boolean isArchive(File appFile, String appPath) {
        if ( appPath.toLowerCase().endsWith(XML_SUFFIX) ) {
            return false;
        } else if ( !appFile.isFile() ) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Create deployment information for a java enterprise application.
     *
     * A location must be specified for the application.  The location must
     * have an java enterprise application archive (an EAR file), an expanded
     * enterprise archive, or an XML loose configuration file.
     *
     * If expansion is enabled, and if the application location holds an EAR file,
     * expand the application to the expanded applications location.
     *
     * @param appInfo Information for the application for which to create
     *     deployment information.
     * @return Deployment information for the application.
     *
     * @throws UnableToAdaptException Thrown if the deployment information
     *     count not be created.
     */
    @Override
    public DeployedAppInfo createDeployedAppInfo(ApplicationInformation<DeployedAppInfo> appInfo)
        throws UnableToAdaptException {

        String appPid = appInfo.getPid(); 
        String appName = appInfo.getName();
        String appPath = appInfo.getLocation();
        File appFile = new File(appPath);

        Tr.debug(_tc, "Create deployed application:" +
                      " PID [ " + appPid + " ]" +
                      " Name [ " + appName + " ]" +
                      " Location [ " + appPath + " ]");

        Container appContainer = appInfo.getContainer();
        Container originalAppContainer = null;

        // If enabled by an attribute of the application manager configuration,
        // expand the application.
        //
        // Expansion is possible if the application file is not already a directory, and is
        // not loosely configuration.

        if ( applicationManager.getExpandApps() && isArchive(appFile, appPath) ) {
            try {
                prepareExpansion();

                WsResource expandedResource = resolveExpansion(appName);
                File expandedFile = expandedResource.asFile();
                expand(appName, appFile, expandedResource, expandedFile);

                originalAppContainer = appContainer;
                appContainer = setupContainer(appPid, expandedFile);

            } catch ( IOException e ) {
                Tr.error(_tc, "warning.could.not.expand.application", appName, e.getMessage());
            }
        }

        Application applicationDD;
        try {
            applicationDD = appContainer.adapt(Application.class); // throws UnableToAdaptException
            // Null when there is no application descriptor.
        } catch ( UnableToAdaptException e ) {
            // CWWKZ0113E: Application {0}: Parse error for application descriptor {1}: {2}
            Tr.error(_tc, "error.application.parse.descriptor", appName, "META-INF/application.xml", e);
            throw e;
        }

        InterpretedContainer jeeContainer;
        if ( appContainer instanceof InterpretedContainer ) {
            jeeContainer = (InterpretedContainer) appContainer;
        } else {
            jeeContainer = appContainer.adapt(InterpretedContainer.class);
        }

        // Set a structure helper for modules that might be expanded inside
        // (e.g., x.ear/y.war or x.ear/y.jar/).
        if ( applicationDD == null ) {
            jeeContainer.setStructureHelper( EARStructureHelper.getUnknownRootInstance() );
        } else {
            List<String> modulePaths = new ArrayList<String>();
            for ( Module module : applicationDD.getModules() ) {
                modulePaths.add( module.getModulePath() );
            }
            jeeContainer.setStructureHelper( EARStructureHelper.create(modulePaths) );
        }
        appInfo.setContainer(jeeContainer);

        EARDeployedAppInfo deployedApp =
            new EARDeployedAppInfo(appInfo, applicationDD, this, originalAppContainer);
        appInfo.setHandlerInfo(deployedApp);

        return deployedApp;
    }
}
