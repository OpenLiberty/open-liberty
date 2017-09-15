package com.ibm.ws.microprofile.health.services;

import java.util.Set;

import org.eclipse.microprofile.health.HealthCheckResponse;

/**
 *
 */
public interface HealthExecutor {

    /**
     * Runs any HealthChecks in the given application module and returns a set
     * of responses, one for each HealthCheck. If none are present this method
     * returns an empty set.
     *
     * @param appName
     * @param moduleName
     * @return the (possibly empty) set of Responses
     */
    public Set<HealthCheckResponse> runHealthChecks(String appName, String moduleName) throws HealthCheckBeanCallException;
}