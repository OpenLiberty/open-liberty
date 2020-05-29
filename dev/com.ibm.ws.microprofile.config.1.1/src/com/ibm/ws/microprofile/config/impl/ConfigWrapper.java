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
package com.ibm.ws.microprofile.config.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.Config;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.common.ConfigException;
import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;

/**
 * Wrapper to record which applications are using a Config.
 *
 * This class is not thread-safe and is package scoped only
 */
class ConfigWrapper {

    private static final TraceComponent tc = Tr.register(ConfigWrapper.class);

    private final WebSphereConfig config;
    private final Set<String> applications = new HashSet<>();

    ConfigWrapper(WebSphereConfig config) {
        this.config = config;
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

    void close() {
        this.applications.clear();
        try {
            this.config.close();
        } catch (IOException e) {
            throw new ConfigException(Tr.formatMessage(tc, "could.not.close.CWMCG0004E", e));
        }
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
}
