package io.openliberty.microprofile.openapi20.internal.utils;

import org.eclipse.microprofile.openapi.models.OpenAPI;

/**
 * Service interface for traversing an OpenAPI model
 */
public interface OpenAPIModelWalker {

    /**
     * Current context of the OpenAPIModelWalker. An instance of Context
     * is passed to every method of the visitor or filter.
     */
    interface Context {
    
        /**
         * Returns the OpenAPI model.
         */
        OpenAPI getModel();
    
        /**
         * Returns the parent of the current object being visited.
         */
        Object getParent();
    
        /**
         * Returns the location of the current object being visited.
         * The format of this location is a JSON pointer.
         */
        String getLocation();
    
        /**
         * Returns the location of the current object being visited
         * with the suffix parameter appended to the end of the
         * string. The format of this location is a JSON pointer.
         */
        String getLocation(String suffix);
    }

    /**
     * Conducts a complete traversal of the OpenAPI model and reports each object to the {@code visitor}.
     *
     * @param openAPI the model to traverse
     * @param visitor the visitor to call
     */
    public void walk(OpenAPI openAPI, OpenAPIModelVisitor visitor);
}
