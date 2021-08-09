package com.ibm.ws.management.j2ee.mejb.service;

/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.management.j2ee.Management;
import javax.management.j2ee.ManagementHome;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.osgi.EJBContainer;
import com.ibm.ws.ejbcontainer.osgi.EJBRemoteRuntime;
import com.ibm.ws.ejbcontainer.osgi.EJBSystemBeanConfig;
import com.ibm.ws.ejbcontainer.osgi.EJBSystemModule;
import com.ibm.ws.kernel.feature.ServerStarted;
import com.ibm.ws.management.j2ee.mejb.ManagementEJB;

/**
 * The ManagementEJB service registers the ManagementEJB Bean to the container so users can lookup ManagementHome.
 * 
 */
@Component(service = { ManagementEJBService.class },
                configurationPid = "com.ibm.ws.management.j2ee.mejb.service.ManagementEJBService",
                configurationPolicy = ConfigurationPolicy.IGNORE,
                immediate = true,
                property = { "service.vendor=IBM" })
public class ManagementEJBService {

    private static final TraceComponent tc = Tr.register(ManagementEJBService.class, "mejb", "com.ibm.ws.management.j2ee.mejb.resources.Messages");

    private static final String MODULE_NAME = "MEJB";
    private static final String BEAN_NAME = "ManagementEJB"; //EJB name
    private static final String BINDING_NAME = "ejb/mejb/MEJB"; //remote home CosNaming binding name
    private EJBSystemModule ejbSystemModule;
    private EJBContainer ejbContainer;
    private EJBRemoteRuntime ejbRemoteRuntime;

    /**
     * True when the server is in the 'started' state.
     */
    private volatile boolean isServerStarted;

    @Reference(service = EJBContainer.class)
    protected synchronized void setEJBContainer(EJBContainer reference) {
        this.ejbContainer = reference;

        // Start SystemModule if everything else is ready
        startManagementEJB();
    }

    protected synchronized void unsetEJBContainer(EJBContainer reference) {
        // Stop Management EJB if not already stopped
        stopManagementEJB();
        this.ejbContainer = null;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected synchronized void setEJBRemoteRuntime(EJBRemoteRuntime ejbRemoteRuntime) {
        this.ejbRemoteRuntime = ejbRemoteRuntime;

        // Start SystemModule if everything else is ready
        startManagementEJB();
    }

    protected synchronized void unsetEJBRemoteRuntime(EJBRemoteRuntime ejbRemoteRuntime) {
        // Stop Management EJB if not already stopped
        stopManagementEJB();

        this.ejbRemoteRuntime = null;
    }

    /**
     * Declarative services method that is invoked once the ServerStarted service
     * is available. Only after this method is invoked is the Management EJB system
     * module started.
     * 
     * @param serverStarted The server started instance
     */
    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected synchronized void setServerStarted(ServerStarted serverStarted) {
        isServerStarted = true;

        // Start SystemModule if everything else is ready
        startManagementEJB();
    }

    /**
     * Declarative services method for unsetting the ServerStarted service instance.
     * 
     * @param serverStarted The Started service instance
     */
    protected synchronized void unsetServerStarted(ServerStarted serverStarted) {
        // Stop Management EJB if not already stopped
        stopManagementEJB();

        isServerStarted = false;
    }

    /**
     * Get the configuration for the EJBs in this module
     * EJBSystemBeanConfig(String name, Class<?> ejbClass, String remoteHomeBindingName,
     * Class<? extends EJBHome> remoteHomeInterface, Class<? extends EJBObject> remoteInterface)
     * 
     * @param name the valid EJB name (e.g., "MyBean"), which is part of the
     *            persistent identity of EJB references and must not be changed
     *            or else serialized proxies will no longer work
     * @param ejbClass the implementation class
     * @param remoteHomeBindingName the CosNaming binding name (e.g., "ejb/test/MyBean")
     * @param remoteHomeInterface the remote home interface
     * @param remoteInterface the remote component interface}
     * 
     * @return array of EJBSystemBeanConfig
     */
    private EJBSystemBeanConfig[] getEJBSystemBeanConfigs() {
        EJBSystemBeanConfig ejb = new EJBSystemBeanConfig(
                        BEAN_NAME,
                        ManagementEJB.class,
                        BINDING_NAME,
                        ManagementHome.class,
                        Management.class);
        EJBSystemBeanConfig[] ejbs = new EJBSystemBeanConfig[] { ejb };
        return ejbs;
    }

    /*
     * Starts system management EJB modules
     * EJBSystemModule startSystemModule(String moduleName, ClassLoader classLoader, EJBSystemBeanConfig[] ejbs);
     * {@param moduleName the name of the module (e.g., "MyEJB.jar"), which must be unique in the server,
     * is part of the persistent identity of references to the EJBs in this module, and must not be changed
     * or else serialized proxies to EJBs will no longer work
     * 
     * @param classLoader the module class loader
     * 
     * @param ejbs the configuration for the EJBs in this module
     * 
     * @return a system module handle, which must be stopped when the system EJBs should no longer be accessible
     */
    private void startManagementEJB() {
        if (isServerStarted && ejbRemoteRuntime != null && ejbContainer != null) {
            ClassLoader classLoader = ManagementEJBService.class.getClassLoader();
            EJBSystemBeanConfig[] ejbs = getEJBSystemBeanConfigs();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ejbs " + ejbs.length);
            }
            ejbSystemModule = ejbContainer.startSystemModule(MODULE_NAME, classLoader, ejbs, ejbRemoteRuntime);

            //EJB is started and registered with EJBContainer       
            if (tc.isDebugEnabled()) {
                Tr.info(tc, "MANAGEMENT_EJB_SERVICE_ACTIVATED");
            }
        }
    }

    private void stopManagementEJB() {
        // Stop system EJB module when if it is in started state
        if (ejbSystemModule != null) {
            ejbSystemModule.stop();
            // Null reference so as not to stop repeatedly
            ejbSystemModule = null;

            if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
                Tr.info(tc, "MANAGEMENT_EJB_SERVICE_DEACTIVATED");
            }
        }
    }
}