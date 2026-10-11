/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.internal.services.OpenAPIVersionConfig;
import io.openliberty.microprofile.openapi20.internal.utils.MessageConstants;

/**
 * Base implementation of OpenAPIVersionConfig which supports configuring version 3.0.x of OpenAPI
 */
public class OpenAPIVersionConfigImpl implements OpenAPIVersionConfig {
    private static final TraceComponent tc = Tr.register(OpenAPIVersionConfigImpl.class);

    private static final String VERSION_KEY = "openAPIVersion";
    protected static final OpenAPIVersion VERSION_30 = new OpenAPIVersion(3, 0, -1);
    protected static final OpenAPIVersion VERSION_303 = new OpenAPIVersion(3, 0, 3);

    /**
     * The OpenAPI version configured by the user, or the result of {@link #defaultVersion()} if a valid version was not configured.
     * <p>
     * This value may be updated at any time in response to configuration changes.
     * <p>
     * This value will always be either a result from {@link #defaultVersion()} or a value for which {@link #isSupported(OpenAPIVersion)} returns {@code true}.
     */
    protected volatile OpenAPIVersion configuredVersion = defaultVersion();
    protected String configuredVersionString = null;

    @Activate
    @Modified
    protected void activate(Map<?, ?> properties) {
        String versionString = (String) properties.get(VERSION_KEY);

        if (Objects.equals(versionString, configuredVersionString)) {
            // Our config has not changed, return immediately to avoid potentially logging warnings
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Config unchanged", versionString);
            }
            return;
        }

        configuredVersionString = versionString;

        if (versionString == null) {
            this.configuredVersion = defaultVersion();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "No OpenAPI version set, default version used", this.configuredVersion);
            }
            return;
        }

        Optional<OpenAPIVersion> version = OpenAPIVersion.parse(versionString);
        if (!version.isPresent()) {
            this.configuredVersion = defaultVersion();
            Tr.warning(tc,
                       MessageConstants.OPENAPI_VERSION_INVALID_CWWK06181W,
                       versionString,
                       getSupportedVersions(),
                       this.configuredVersion);
            return;
        }

        if (!isSupported(version.get())) {
            this.configuredVersion = defaultVersion();
            Tr.warning(tc,
                       MessageConstants.OPENAPI_VERSION_NOT_SUPPORTED_CWWK06182W,
                       version.get(),
                       getSupportedVersions(),
                       this.configuredVersion);
            return;
        }

        // Configured version is acceptable
        this.configuredVersion = version.get();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "OpenAPI version set", this.configuredVersion);
        }
    }

    @Override
    public void applyConfig(OpenAPI model) {
        // Get the version out of the model
        Optional<OpenAPIVersion> modelVersion = OpenAPIVersion.parse(model.getOpenapi());

        // Check if it needs replaced
        boolean replace = modelVersion.map(this::requiresReplacement)
                                      .orElse(true);

        if (replace) {
            // Update the version in the model
            model.setOpenapi(getReplacementVersion().toString());
        }
    }

    @Override
    public OpenAPIVersion getVersion() {
        return getReplacementVersion();
    }

    /**
     * Returns the default value for the configured OpenAPI version.
     * <p>
     * Used in cases where a valid and supported version is not configured
     * <p>
     * Subclasses should override this to set the default configured version for their feature.
     *
     * @return the default OpenAPI version
     */
    protected OpenAPIVersion defaultVersion() {
        return VERSION_30;
    }

    /**
     * Returns whether a configured OpenAPI version is supported by this version of the mpOpenAPI feature
     * <p>
     * Subclasses should override this to accept the versions of OpenAPI that they support.
     *
     * @param version the version to check
     * @return {@code true} if this is a supported version, {@code false} otherwise
     */
    protected boolean isSupported(OpenAPIVersion version) {
        return version.getMajor() == 3 && version.getMinor() == 0;
    }

    /**
     * Lists the supported OpenAPI spec versions for output in error messages.
     * <p>
     * These are the versions that the user may configure in the server.xml.
     * <p>
     * Subclasses should override this to indicate which versions of OpenAPI they support.
     *
     * @return a comma separated list of OpenAPI spec versions which are supported
     */
    protected String getSupportedVersions() {
        return "3.0, 3.0.x";
    }

    /**
     * Returns whether an OpenAPIVersion parsed from the model should be replaced, based on the configured version
     *
     * @param modelVersion the version parsed from the model
     * @return {@code true} if the version should be updated, {@code false} otherwise
     */
    protected boolean requiresReplacement(OpenAPIVersion modelVersion) {

        if (modelVersion.getPatch() < 0) {
            // Model requires a three digit version, so must replace if it only has two digits
            return true;
        }

        if (modelVersion.getMajor() != configuredVersion.getMajor()
            || modelVersion.getMinor() != configuredVersion.getMinor()) {
            // Major and minor version must match configured version, so replace if either doesn't
            return true;
        }

        if (configuredVersion.getPatch() != -1
            && modelVersion.getPatch() != configuredVersion.getPatch()) {
            // If the configured version has a patch component, the model version must match it
            return true;
        }

        // Otherwise, the model version matches the configured version and doesn't need replaced
        return false;
    }

    /**
     * Returns the version that should be set into the {@code OpenAPI} model.
     * <p>
     * To be a valid version, the value returned here must be of the form "a.b.c".
     * <p>
     * Subclasses should override this to do any mapping required between the configured version and the version that should be set into the model.
     *
     * @return the version to insert into the model
     */
    protected OpenAPIVersion getReplacementVersion() {
        if (configuredVersion.getPatch() >= 0) {
            return configuredVersion;
        } else {
            return VERSION_303;
        }
    }

}
