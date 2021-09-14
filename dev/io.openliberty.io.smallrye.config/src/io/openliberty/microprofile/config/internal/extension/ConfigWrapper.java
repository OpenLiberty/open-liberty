/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.extension;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.config.Config;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Wrapper to record which applications are using a Config.
 *
 * This class is not thread-safe and is package scoped only
 */
@Trivial
class ConfigWrapper {

    //We try to keep track of which application is using which Config. There are cases where the Config is used by a global component
    //or we just can't work out which app it is. Then we fall back to this global name.
    static final String GLOBAL_CONFIG_APPLICATION_NAME = "!GLOBAL_CONFIG_APPLICATION_NAME!";

    private final Config config;
    private final Set<String> applications = new HashSet<>();

    ConfigWrapper(Config config) {
        this.config = config;
    }

    void addApplication(String appName) {
        this.applications.add(appName);
    }

    /**
     * Remove an application from this ConfigWrapper. If only the global app is left, remove that too.
     *
     * @param appName The application to remove
     * @return true if there are no applications left in this ConfigWrapper
     */
    boolean removeApplication(String appName) {
        boolean empty = false;
        this.applications.remove(appName);
        //if all applications except for the global one have been removed, remove that too
        if (this.applications.size() == 1) {
            this.applications.remove(GLOBAL_CONFIG_APPLICATION_NAME);
        }
        if (this.applications.size() == 0) {
            empty = true;
        }
        return empty;
    }

    Set<String> listApplications() {
        return Collections.unmodifiableSet(applications);
    }

    Config getConfig() {
        return this.config;
    }
}
