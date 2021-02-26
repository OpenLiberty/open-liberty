/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.microprofile.health30.services;

import java.util.Set;

import org.eclipse.microprofile.health.HealthCheckResponse;

import com.ibm.ws.microprofile.health.services.HealthCheckBeanCallException;

/**
 * For a given application, invoke all its HealthCheck beans and aggregate response
 */
public interface HealthCheck30CDIBeanInvoker {

    /**
     * Will look up BeanManager in the current context and invoke all HealthCheck beans,
     * based on the provided health check procedure, aggregating the result.
     *
     * @param healthCheckProcedure
     *
     * @return aggregate result (any DOWN means aggregate response is DOWN)
     */
    public Set<HealthCheckResponse> checkAllBeans(String appName, String moduleName, String healthCheckProcedure) throws HealthCheckBeanCallException;

    /**
     * Removes references to an application module. Currently this operation removes entries from the
     * BeanManager cache that is maintained to avoid looking it up all the time.
     *
     * @param appName
     * @param moduleName
     */
    void removeModuleReferences(String appName, String moduleName);
}
