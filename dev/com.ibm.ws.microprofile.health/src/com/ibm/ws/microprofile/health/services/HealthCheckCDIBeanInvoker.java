package com.ibm.ws.microprofile.health.services;

import java.util.Set;

import org.eclipse.microprofile.health.HealthCheckResponse;

/**
 * For a given application, invoke all its HealthCheck beans and aggregate response
 */
public interface HealthCheckCDIBeanInvoker {

    /**
     * Will look up BeanManager in the current context and invoke all HealthCheck beans,
     * aggregating the result.
     *
     * @return aggregate result (any DOWN means aggregate response is DOWN)
     */
    public Set<HealthCheckResponse> checkAllBeans(String appName, String moduleName) throws HealthCheckBeanCallException;

    /**
     * Removes references to an application module. Currently this operation removes entries from the
     * BeanManager cache that is maintained to avoid looking it up all the time.
     *
     * @param appName
     * @param moduleName
     */
    void removeModuleReferences(String appName, String moduleName);
}
