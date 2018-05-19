/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.container;

import com.ibm.ws.app.manager.springboot.container.config.ServerConfiguration;
import com.ibm.ws.app.manager.springboot.container.config.SpringConfiguration;
import com.ibm.ws.app.manager.springboot.support.ContainerInstanceFactory;

/**
 * Spring Boot configuration that configures a embedded container such as a
 * web container or a reactive container.
 */
public interface SpringBootConfig {
    /**
     * Configures an embedded configuration based on the given server
     * configuration. The given server configuration is intended to
     * configure an http endpoint for the Spring Boot application.
     * <p>
     * For web container configuration it is expected that the
     * ServletContext will be fully initialized by the exit of
     * this method. The http endpoint must not be listening
     * on the configured port until start is called.
     * <p>
     * A {@link ContainerInstanceFactory} is looked up according
     * to the factoryParamType and called to create instances
     * for the configuration.
     *
     * @param config liberty server configuration
     * @param factoryParam a factory specific parameter
     * @param factoryParamtype the helper specific parameter type
     * @param additionalAppConfiguration Spring boot application config
     */
    <T> void configure(ServerConfiguration config, T factoryParam, Class<T> factoryParamType, SpringConfiguration additionalAppConfiguration);

    /**
     * Starts the http endpoint for this configuration
     */
    void start();

    /**
     * Stops and destroys the configuration. For web container
     * configuration this includes destroying the ServletContext
     * and stopping the http endpoint.
     */
    void stop();

    /**
     * Spring boot config id
     *
     * @return Spring boot config id
     */
    String getId();
}
