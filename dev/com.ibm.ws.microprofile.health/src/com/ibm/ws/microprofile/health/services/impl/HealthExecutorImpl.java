package com.ibm.ws.microprofile.health.services.impl;

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
    private HealthCheckCDIBeanInvoker healthCheckCDIBeanInvoker;
    private J2EENameFactory j2eeNameFactory;
    private final static Logger logger = Logger.getLogger(HealthExecutorImpl.class.getName(), "com.ibm.ws.microprofile.health.resources.Health");

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

        // TaskIdentity identifies the task for the purposes of mgmt/auditing.
        execProps.put(MANAGEDTASK_IDENTITY_NAME, HC_MANAGEDTASK_IDENTITY_NAME);

        // TaskOwner identifies the submitter of the task.
        execProps.put(WSContextService.TASK_OWNER, HC_TASK_OWNER);

        HealthCheckCDIBeanInvoker proxy = appModuleContextService.createContextualProxy(execProps, j2eeName, healthCheckCDIBeanInvoker, HealthCheckCDIBeanInvoker.class);

        try {
            return proxy.checkAllBeans();
        } catch (HealthCheckBeanCallException e) {
            logger.log(Level.SEVERE, "healthcheck.bean.call.exception.CWMH0050E", new Object[] { e.getBeanName(),
                                                                                                 appName,
                                                                                                 moduleName,
                                                                                                 e.getCause().toString(),
                                                                                                 e.getMessage() });
            throw e;
        }
    }

}
