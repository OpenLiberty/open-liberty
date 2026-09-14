/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.config.fat.tests.visibility.earlib;

import static com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.VisibilityTestConstants.EAR_LIB_CONFIG_PROPERTY;
import static com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.VisibilityTestConstants.EJB_JAR_CONFIG_PROPERTY;
import static com.ibm.ws.microprofile.config.fat.tests.visibility.earlib.VisibilityTestConstants.WAR_CONFIG_PROPERTY;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class EarLibBean {

    @Inject
    private Config injectedConfig;

    @Inject
    @ConfigProperty(name = WAR_CONFIG_PROPERTY)
    private Optional<String> warProperty;

    @Inject
    @ConfigProperty(name = EJB_JAR_CONFIG_PROPERTY)
    private Optional<String> ejbJarProperty;

    @Inject
    @ConfigProperty(name = EAR_LIB_CONFIG_PROPERTY)
    private Optional<String> earLibProperty;

    public Config getConfig() {
        return ConfigProvider.getConfig();
    }

    public Config getInjectedConfig() {
        return injectedConfig;
    }

    public Optional<String> getWarProperty() {
        return warProperty;
    }

    public Optional<String> getEjbJarProperty() {
        return ejbJarProperty;
    }

    public Optional<String> getEarLibProperty() {
        return earLibProperty;
    }
}
