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

    private final static Map<String, Long> timestamps = new HashMap<String, Long>();

    protected ModuleHandler webModuleHandler;
    protected ModuleHandler ejbModuleHandler;
    protected ModuleHandler clientModuleHandler;
    protected ModuleHandler connectorModuleHandler;
    protected DeployedAppMBeanRuntime appMBeanRuntime;

    private ServiceReference<JavaEEVersion> versionRef;
    protected volatile Version platformVersion = JavaEEVersion.DEFAULT_VERSION;

    private ApplicationManager applicationManager;
    private final ZipUtils zipUtils = new ZipUtils();

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

    @Override
    public DeployedAppInfo createDeployedAppInfo(ApplicationInformation<DeployedAppInfo> applicationInformation) throws UnableToAdaptException {
        Container applicationContainer = applicationInformation.getContainer();
        Container originalContainer = null;

        try {
            // Check whether we need to expand this EAR
            if (applicationManager.getExpandApps()) {

                // Make sure this is a file and not an expanded directory
                String location = applicationInformation.getLocation();
                File earFile = new File(location);
                if (earFile.isFile() && !location.toLowerCase().endsWith(XML_SUFFIX)) {

                    // Make sure the apps/expanded directory is available
                    WsResource expandedAppsDir = getLocationAdmin().resolveResource(EXPANDED_APPS_DIR);
                    expandedAppsDir.create();

                    // Store the timestamp for the EAR file and get the current value (if it exists)
                    Long earFileTimestamp = timestamps.put(earFile.getAbsolutePath(), earFile.lastModified());

                    // If the expanded directory exists, delete it.
                    WsResource expandedEarDir = getLocationAdmin().resolveResource(EXPANDED_APPS_DIR + applicationInformation.getName() + ".ear/");
                    if (expandedEarDir.exists()) {
                        // If the expanded EAR directory already exists we need to try to figure out if this was an update to the EAR file in apps/dropins
                        // or an update to the expanded directory. We do this by checking the EAR file timestamp against a stored value. 
                        //
                        // Doing it this way is really unfortunate, but it seems to be the best option at the moment. 
                        // TODO - Either figure out a legitimate way to use Notifier to determine which container changed, or
                        // improve by cachine the timestamp values

                        // If we don't have a timestampe for the EAR, or the ear file has been changed, delete the expanded directory
                        if (earFileTimestamp == null || earFileTimestamp.longValue() != earFile.lastModified()) {
                            zipUtils.recursiveDelete(expandedEarDir.asFile());
                            expandedEarDir.create();
                            zipUtils.unzip(earFile, expandedEarDir.asFile());
                        }
                    } else {
                        // The expanded directory doesn't exist yet, so create it and unzip the EAR (and any contained WAR files)                 
                        expandedEarDir.create();
                        zipUtils.unzip(earFile, expandedEarDir.asFile());
                    }

                    originalContainer = applicationContainer;
                    applicationContainer = setupContainer(applicationInformation.getPid(), expandedEarDir.asFile());

                }

            }
        } catch (IOException ex) {
            // Log error and continue to use the container for the EAR file           
            Tr.error(_tc, "warning.could.not.expand.application", applicationInformation.getName(), ex.getMessage());
        }

        // An exception means there was a parse error; a null value means that there was
        // no descriptor at all.
        Application applicationDD;
        try {
            applicationDD = applicationContainer.adapt(Application.class); // throws UnableToAdaptException
        } catch (UnableToAdaptException e) {
            // error.application.parse.descriptor=
            // CWWKZ0113E: Application {0}: Parse error for application descriptor {1}: {2}            
            Tr.error(_tc, "error.application.parse.descriptor", applicationInformation.getName(), "META-INF/application.xml", e);
            throw e;
        }

        InterpretedContainer jeeContainer;
        if (applicationContainer instanceof InterpretedContainer) {
            jeeContainer = (InterpretedContainer) applicationContainer;
        } else {
            jeeContainer = applicationContainer.adapt(InterpretedContainer.class);
        }
        // Set a structure helper for modules that might be expanded inside
        // (e.g., x.ear/y.war or x.ear/y.jar/).
        if (applicationDD == null) {
            jeeContainer.setStructureHelper(EARStructureHelper.getUnknownRootInstance());
        } else {
            List<String> modulePaths = new ArrayList<String>();
            for (Module module : applicationDD.getModules()) {
                modulePaths.add(module.getModulePath());
            }
            jeeContainer.setStructureHelper(EARStructureHelper.create(modulePaths));
        }
        applicationInformation.setContainer(jeeContainer);

        EARDeployedAppInfo deployedApp = new EARDeployedAppInfo(applicationInformation, applicationDD, this, originalContainer);
        applicationInformation.setHandlerInfo(deployedApp);
        return deployedApp;
    }
}
