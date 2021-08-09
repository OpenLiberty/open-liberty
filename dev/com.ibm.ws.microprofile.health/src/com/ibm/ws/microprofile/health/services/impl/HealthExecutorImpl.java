/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health.services.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.ws.microprofile.health.services.HealthCheckBeanCallException;
import com.ibm.ws.microprofile.health.services.HealthCheckCDIBeanInvoker;
import com.ibm.ws.microprofile.health.services.HealthExecutor;
import com.ibm.wsspi.threadcontext.WSContextService;

@Component(service = HealthExecutor.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class HealthExecutorImpl implements HealthExecutor {

    /**
     * For retrieving the CMD and classloader context for a given
     * app/module name.
     */
    private AppModuleContextService appModuleContextService;

    /**
     * Jakarta EE version if Jakarta EE 9 or higher. If 0, assume a lesser EE spec version.
     */
    private volatile int eeVersion;

    /**
     * Tracks the most recently bound EE version service reference. Only use this within the set/unsetEEVersion methods.
     */
    private ServiceReference<JavaEEVersion> eeVersionRef;

    private HealthCheckCDIBeanInvoker healthCheckCDIBeanInvoker;
    private J2EENameFactory j2eeNameFactory;
    private final static Logger logger = Logger.getLogger(HealthExecutorImpl.class.getName(), "com.ibm.ws.microprofile.health.resources.Health");

    private final static String HC_MANAGEDTASK_IDENTITY_NAME = "mp.healthcheck.proxy";
    private final static String HC_TASK_OWNER = "mp.healthcheck.runtime";
    private final static String ONLY_WAR_EJB_NOT_SUPPORTED = null;

    @Reference
    protected void setAppModuleContextService(AppModuleContextService appModuleContextService) {
        this.appModuleContextService = appModuleContextService;
    }

    @Reference
    protected void setHealthCheckApplicationBeanInvoker(HealthCheckCDIBeanInvoker healthCheckCDIBeanInvoker) {
        this.healthCheckCDIBeanInvoker = healthCheckCDIBeanInvoker;
    }

    @Reference
    protected void setJ2EENameFactory(J2EENameFactory j2eeNameFactory) {
        this.j2eeNameFactory = j2eeNameFactory;
    }

    @Override
    public Set<HealthCheckResponse> runHealthChecks(String appName, String moduleName) throws HealthCheckBeanCallException {

        J2EEName j2eeName = j2eeNameFactory.create(appName, moduleName, ONLY_WAR_EJB_NOT_SUPPORTED);

        Map<String, String> execProps = new HashMap<String, String>();
        Set<HealthCheckResponse> retval;

        // TaskIdentity identifies the task for the purposes of mgmt/auditing.
        final String MANAGEDTASK_IDENTITY_NAME = eeVersion < 9 ? "javax.enterprise.concurrent.IDENTITY_NAME" : "jakarta.enterprise.concurrent.IDENTITY_NAME";
        execProps.put(MANAGEDTASK_IDENTITY_NAME, HC_MANAGEDTASK_IDENTITY_NAME);

        // TaskOwner identifies the submitter of the task.
        execProps.put(WSContextService.TASK_OWNER, HC_TASK_OWNER);

        HealthCheckCDIBeanInvoker proxy = appModuleContextService.createContextualProxy(execProps, j2eeName, healthCheckCDIBeanInvoker, HealthCheckCDIBeanInvoker.class);

        try {
            retval = proxy.checkAllBeans(appName, moduleName);
        } catch (HealthCheckBeanCallException e) {
            logger.log(Level.SEVERE, "healthcheck.bean.call.exception.CWMH0050E", new Object[] { e.getBeanName(),
                                                                                                 appName,
                                                                                                 moduleName,
                                                                                                 e.getCause().toString(),
                                                                                                 e.getMessage() });
            throw e;
        }

        for (HealthCheckResponse hcr : retval) {
            if (HealthCheckResponse.State.DOWN == hcr.getState()) {
                logger.log(Level.WARNING, "healthcheck.application.down.CWMH0051W", new Object[] { hcr.getClass().toString(),
                                                                                                   appName,
                                                                                                   moduleName,
                                                                                                   hcr.getState().toString(),
                                                                                                   hcr.getData() != null ? hcr.getData().toString() : "{NO DATA}" });
            }
        }
        return retval;
    }

    @Override
    public void removeModuleReferences(String appName, String moduleName) {
        healthCheckCDIBeanInvoker.removeModuleReferences(appName, moduleName);
    }

    /**
     * Declarative Services method for setting the Jakarta/Java EE version
     *
     * @param ref reference to the service
     */
    @Reference(service = JavaEEVersion.class, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        String version = (String) ref.getProperty("version");
        if (version == null) {
            eeVersion = 0;
        } else {
            int dot = version.indexOf('.');
            String major = dot > 0 ? version.substring(0, dot) : version;
            eeVersion = Integer.parseInt(major);
        }
        eeVersionRef = ref;
    }

    /**
     * Declarative Services method for unsetting the Jakarta/Java EE version
     *
     * @param ref reference to the service
     */
    protected void unsetEEVersion(ServiceReference<JavaEEVersion> ref) {
        if (eeVersionRef == ref) {
            eeVersionRef = null;
            eeVersion = 0;
        }
    }
}
