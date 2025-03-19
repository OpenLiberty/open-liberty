/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.services.impl;

import org.osgi.service.component.annotations.Component;

import io.openliberty.microprofile.openapi20.internal.OpenAPIVersion;
import io.openliberty.microprofile.openapi20.internal.OpenAPIVersionConfigImpl;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIVersionConfig;

/**
 * Implementation of OpenAPIVersionConfig which supports configuring version 3.0.x and 3.1.x of OpenAPI
 */
@Component(configurationPid = "io.openliberty.microprofile.openapi", service = OpenAPIVersionConfig.class)
public class OpenAPIVersionConfig40Impl extends OpenAPIVersionConfigImpl {

    protected static final OpenAPIVersion VERSION_31 = new OpenAPIVersion(3, 1, -1);
    protected static final OpenAPIVersion VERSION_310 = new OpenAPIVersion(3, 1, 0);

    @Override
    protected boolean isSupported(OpenAPIVersion version) {
        return version.getMajor() == 3
               && (version.getMinor() == 0 || version.getMinor() == 1);
    }

    @Override
    protected OpenAPIVersion getReplacementVersion() {
        // If the user has configured a two-digit version, convert to the corresponding three-digit version
        if (configuredVersion.equals(VERSION_30)) {
            return VERSION_303;
        } else if (configuredVersion.equals(VERSION_31)) {
            return VERSION_310;
        } else {
            return configuredVersion;
        }
    }

    @Override
    protected OpenAPIVersion defaultVersion() {
        return VERSION_31;
    }

    @Override
    protected String getSupportedVersions() {
        return "3.1, 3.1.x, 3.0, 3.0.x";
    }

}
