/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.microprofile.health30.services.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.ws.microprofile.health.services.HealthCheckBeanCallException;
import com.ibm.ws.microprofile.health.services.impl.AppModuleContextService;
import com.ibm.wsspi.threadcontext.WSContextService;

import io.openliberty.microprofile.health30.services.HealthCheck30CDIBeanInvoker;
import io.openliberty.microprofile.health30.services.HealthCheck30Executor;

@Component(service = HealthCheck30Executor.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class HealthCheck30ExecutorImpl implements HealthCheck30Executor {

    /**
     * For retrieving the CMD and classloader context for a given
     * app/module name.
     */
    private AppModuleContextService appModuleContextService;

    private HealthCheck30CDIBeanInvoker healthCheckCDIBeanInvoker;
    private J2EENameFactory j2eeNameFactory;
    private final static Logger logger = Logger.getLogger(HealthCheck30ExecutorImpl.class.getName(), "io.openliberty.microprofile.health.resources.Health");

    /**
     * For creating J2EENames.
     */
    private static final String MANAGEDTASK_IDENTITY_NAME = "javax.enterprise.concurrent.IDENTITY_NAME";
    private final static String HC_MANAGEDTASK_IDENTITY_NAME = "mp.healthcheck.proxy";
    private final static String HC_TASK_OWNER = "mp.healthcheck.runtime";
    private final static String ONLY_WAR_EJB_NOT_SUPPORTED = null;

    @Reference
    protected void setAppModuleContextService(AppModuleContextService appModuleContextService) {
        this.appModuleContextService = appModuleContextService;
    }

    @Reference
    protected void setHealthCheckApplicationBeanInvoker(HealthCheck30CDIBeanInvoker healthCheckCDIBeanInvoker) {
        this.healthCheckCDIBeanInvoker = healthCheckCDIBeanInvoker;
    }

    @Reference
    protected void setJ2EENameFactory(J2EENameFactory j2eeNameFactory) {
        this.j2eeNameFactory = j2eeNameFactory;
    }

    @Override
    public Set<HealthCheckResponse> runHealthChecks(String appName, String moduleName, String healthCheckProcedure) throws HealthCheckBeanCallException {

        J2EEName j2eeName = j2eeNameFactory.create(appName, moduleName, ONLY_WAR_EJB_NOT_SUPPORTED);

        Map<String, String> execProps = new HashMap<String, String>();
        Set<HealthCheckResponse> retval;

        // TaskIdentity identifies the task for the purposes of mgmt/auditing.
        execProps.put(MANAGEDTASK_IDENTITY_NAME, HC_MANAGEDTASK_IDENTITY_NAME);

        // TaskOwner identifies the submitter of the task.
        execProps.put(WSContextService.TASK_OWNER, HC_TASK_OWNER);

        HealthCheck30CDIBeanInvoker proxy = appModuleContextService.createContextualProxy(execProps, j2eeName, healthCheckCDIBeanInvoker, HealthCheck30CDIBeanInvoker.class);

        try {
            retval = proxy.checkAllBeans(appName, moduleName, healthCheckProcedure);
        } catch (HealthCheckBeanCallException e) {
            logger.log(Level.SEVERE, "healthcheck.bean.call.exception.CWMMH0050E", new Object[] { e.getBeanName(),
                                                                                                  appName,
                                                                                                  moduleName,
                                                                                                  e.getCause().toString(),
                                                                                                  e.getMessage() });
            throw e;
        }

        for (HealthCheckResponse hcr : retval) {
            if (HealthCheckResponse.Status.DOWN == hcr.getStatus()) {
                logger.log(Level.WARNING, "healthcheck.application.down.CWMMH0052W", new Object[] { hcr.getClass().toString(),
                                                                                                    appName,
                                                                                                    moduleName,
                                                                                                    hcr.getStatus().toString(),
                                                                                                    hcr.getData() != null ? hcr.getData().toString() : "{NO DATA}" });
            }
        }
        return retval;
    }

    @Override
    public void removeModuleReferences(String appName, String moduleName) {
        healthCheckCDIBeanInvoker.removeModuleReferences(appName, moduleName);
    }
}
