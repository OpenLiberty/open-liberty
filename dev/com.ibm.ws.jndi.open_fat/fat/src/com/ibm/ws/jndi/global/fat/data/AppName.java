/*
 * =============================================================================
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.ws.jndi.global.fat.data;

/**
 *
 */
public enum AppName {
    JNDI_GLOBAL("jndi-global"), PARENTLAST("parentLast");

    private final String appName;

    private AppName(final String appName) {
        this.appName = appName;
    }

    public String getAppName() {
        return appName;
    }
}
