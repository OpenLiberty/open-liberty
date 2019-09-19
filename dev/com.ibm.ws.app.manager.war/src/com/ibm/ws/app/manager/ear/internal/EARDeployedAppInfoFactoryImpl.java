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
import com.ibm.ws.app.manager.internal.AppManagerConstants;
import com.ibm.ws.app.manager.module.AbstractDeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.DeployedAppInfo;
import com.ibm.ws.app.manager.module.DeployedAppInfoFactory;
import com.ibm.ws.app.manager.module.DeployedAppMBeanRuntime;
import com.ibm.ws.app.manager.module.DeployedAppServices;
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
public class EARDeployedAppInfoFactoryImpl extends AbstractDeployedAppInfoFactory implements DeployedAppInfoFactory {
    private static final TraceComponent _tc = Tr.register(EARDeployedAppInfoFactoryImpl.class, new String[] { "webcontainer", "applications", "app.manager" },
                                                          "com.ibm.ws.app.manager.war.internal.resources.Messages",
                                                          "com.ibm.ws.app.manager.ear.internal.EARDeployedAppInfoFactoryImpl");

    @Reference
    protected DeployedAppServices deployedAppServices;

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
        WsResource expansionResource = deployedAppServices.getLocationAdmin().resolveResource(AppManagerConstants.EXPANDED_APPS_DIR);
        expansionResource.create();
    }

    protected WsResource resolveExpansion(String appName) {
        return deployedAppServices.getLocationAdmin().resolveResource(AppManagerConstants.EXPANDED_APPS_DIR + appName + ".ear/");
    }

    // Time stamps of EAR files which have been expanded during this JVM launch..

    // TODO: This will cause a slow memory leak during a long run which adds then removes
    //       a long sequence of applications.
    //
    //       But, there will also be a gradual accumulation of files on disk, as removing
    //       unexpanded EAR files doesn't cause the expansions of those files to be removed.

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
     * @param file    The file which is to be tested.
     *
     * @return The new time stamp of the file, if the file is to be expanded.
     *         Null if the file is not to be expanded.
     */
    private static Long getUpdatedStamp(String absPath, File file) {
        String methodName = "getUpdatedStamp";
        boolean doDebug = _tc.isDebugEnabled();

        Long currentStamp = Long.valueOf(file.lastModified());

        Long newStamp;
        String newStampReason = null;

        synchronized (expansionStamps) {
            Long priorStamp = expansionStamps.put(absPath, currentStamp);

            if (priorStamp == null) {
                newStamp = currentStamp;
                if (doDebug) {
                    newStampReason = "First extraction; stamp [ " + currentStamp + " ]";
                }
            } else if (currentStamp.longValue() != priorStamp.longValue()) {
                newStamp = currentStamp;
                if (doDebug) {
                    newStampReason = "Additional extraction; old stamp [ " + priorStamp + " ] new stamp [ " + currentStamp + " ]";
                }
            } else {
                newStamp = null;
                if (doDebug) {
                    newStampReason = "No extraction; stamp [ " + currentStamp + " ]";
                }
            }
        }

        if (doDebug) {
            Tr.debug(_tc, methodName + ": " + newStampReason);
        }
        return newStamp;
    }

    private static void clearStamp(String absPath, File file) {
        synchronized (expansionStamps) {
            expansionStamps.remove(absPath);
        }
    }

    protected void expand(
                          String name, File collapsedFile,
                          WsResource expandedResource, File expandedFile) throws IOException {

        String collapsedPath = collapsedFile.getAbsolutePath();

        Long updatedStamp = getUpdatedStamp(collapsedPath, collapsedFile);
        if (updatedStamp == null) {
            // Nothing to do: Already extracted by this JVM, and has the same
            // time stamp as when previously extracted.
            return;
        }

        try {
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

            ZipUtils.unzip(collapsedFile, expandedFile, ZipUtils.IS_EAR, updatedStamp); // throws IOException

        } catch (IOException e) {
            // Forget about the expansion if it failed.
            clearStamp(collapsedPath, collapsedFile);

            throw e;
        }
    }

    /**
     * Create deployment information for a java enterprise application.
     *
     * A location must be specified for the application. The location must
     * have an java enterprise application archive (an EAR file), an expanded
     * enterprise archive, or an XML loose configuration file.
     *
     * If expansion is enabled, and if the application location holds an EAR file,
     * expand the application to the expanded applications location.
     *
     * @param appInfo Information for the application for which to create
     *                    deployment information.
     * @return Deployment information for the application.
     *
     * @throws UnableToAdaptException Thrown if the deployment information
     *                                    count not be created.
     */
    @Override
    public DeployedAppInfo createDeployedAppInfo(ApplicationInformation<DeployedAppInfo> appInfo) throws UnableToAdaptException {

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

        BinaryType appType = getApplicationType(appFile, appPath);
        if (appType == BinaryType.LOOSE) {
            Tr.info(_tc, "info.loose.app", appName, appPath);
        } else if (appType == BinaryType.DIRECTORY) {
            Tr.info(_tc, "info.directory.app", appName, appPath);
        } else if (applicationManager.getExpandApps()) {

            try {
                prepareExpansion();

                WsResource expandedResource = resolveExpansion(appName);
                File expandedFile = expandedResource.asFile();
                Tr.info(_tc, "info.expanding.app", appName, appPath, expandedFile.getAbsolutePath());

                expand(appName, appFile, expandedResource, expandedFile);

                originalAppContainer = appContainer;
                appContainer = deployedAppServices.setupContainer(appPid, expandedFile);

            } catch (IOException e) {
                Tr.error(_tc, "warning.could.not.expand.application", appName, e.getMessage());
            }
        } else {
            Tr.info(_tc, "info.unexpanded.app", appName, appPath);
        }

        Application applicationDD;
        try {
            applicationDD = appContainer.adapt(Application.class); // throws UnableToAdaptException
            // Null when there is no application descriptor.
        } catch (UnableToAdaptException e) {
            // CWWKZ0113E: Application {0}: Parse error for application descriptor {1}: {2}
            Tr.error(_tc, "error.application.parse.descriptor", appName, "META-INF/application.xml", e);
            throw e;
        }

        InterpretedContainer jeeContainer;
        if (appContainer instanceof InterpretedContainer) {
            jeeContainer = (InterpretedContainer) appContainer;
        } else {
            jeeContainer = appContainer.adapt(InterpretedContainer.class);
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
        appInfo.setContainer(jeeContainer);

        EARDeployedAppInfo deployedApp = new EARDeployedAppInfo(appInfo, applicationDD, this, deployedAppServices, originalAppContainer);
        appInfo.setHandlerInfo(deployedApp);

        return deployedApp;
    }
}
