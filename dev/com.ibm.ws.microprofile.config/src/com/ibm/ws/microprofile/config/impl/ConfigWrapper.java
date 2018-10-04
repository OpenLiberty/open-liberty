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

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.Config;

import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;

/**
 * Wrapper to record which applications are using a Config
 */
public class ConfigWrapper implements Closeable {

    private final WebSphereConfig config;
    private final Set<String> applications = new HashSet<>();

    public ConfigWrapper(WebSphereConfig config) {
        this.config = config;
    }

    public void addApplication(String appName) {
        synchronized (this) {
            this.applications.add(appName);
        }
    }

    public boolean removeApplication(String appName) {
        boolean close = false;
        synchronized (this) {
            boolean removed = this.applications.remove(appName);
            if (removed && this.applications.size() == 0) {
                close = true;
            }
        }
        return close;
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws IOException {
        synchronized (this) {
            this.applications.clear();
            this.config.close();
        }
    }

    /**
     * @return
     */
    public Set<String> getApplications() {
        return applications;
    }

    /**
     * @return
     */
    public Config getConfig() {
        return config;
    }
}
