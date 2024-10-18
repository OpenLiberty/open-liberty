package io.openliberty.microprofile.openapi20.internal.services;

import org.eclipse.microprofile.openapi.models.OpenAPI;

/**
 * Applies any version configuration provided by the user.
 */
public interface OpenAPIVersionConfig {

    /**
     * Updates the {@code openapi} property of the model object according to the configuration
     *
     * @param model the {@code OpenAPI} model object to update
     */
    public void applyConfig(OpenAPI model);
}
