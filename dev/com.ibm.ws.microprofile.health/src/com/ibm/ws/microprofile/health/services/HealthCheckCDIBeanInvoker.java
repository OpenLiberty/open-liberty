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
    public Set<HealthCheckResponse> checkAllBeans() throws HealthCheckBeanCallException;
}
