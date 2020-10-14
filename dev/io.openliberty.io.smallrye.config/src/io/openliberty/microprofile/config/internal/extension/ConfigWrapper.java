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
package io.openliberty.microprofile.config.internal.extension;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.Config;

/**
 * Wrapper to record which applications are using a Config.
 *
 * This class is not thread-safe and is package scoped only
 */
class ConfigWrapper {

    private Config config;
    private final ClassLoader classLoader;
    private final Set<String> applications = new HashSet<>();

    ConfigWrapper(Config config, ClassLoader classLoader) {
        this.config = config;
        this.classLoader = classLoader;
    }

    void addApplication(String appName) {
        this.applications.add(appName);
    }

    boolean removeApplication(String appName) {
        boolean close = false;
        boolean removed = this.applications.remove(appName);
        if (removed && (this.applications.size() == 0)) {
            close = true;
        }
        return close;
    }

    /**
     * @return
     */
    Set<String> getApplications() {
        return this.applications;
    }

    /**
     * @return
     */
    Config getConfig() {
        return this.config;
    }

    /**
     * @param config2
     */
    void updateConfig(Config config) {
        this.config = config;
    }

    ClassLoader getClassLoader() {
        return this.classLoader;
    }
}
