/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.validation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.microprofile.openapi.models.Components;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent;
import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent.Severity;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.internal.validation.ReferenceValidator;
import io.openliberty.microprofile.openapi20.internal.validation.ValidationHelper;

/**
 * Validates references for OpenAPI 3.1 documents
 * <p>
 * Main differences from {@link ReferenceValidator} are
 * <ul>
 * <li>Handles {@code PathItem} references
 * <li>Only validates references to {@code #/components/<type>/<name>}. Other references may be valid, but we can't validate them.
 * <li>Validates that the reference is a valid URI
 * </ul>
 */
public class Reference31Validator {

    private static final TraceComponent tc = Tr.register(Reference31Validator.class);

    private static final Reference31Validator INSTANCE = new Reference31Validator();

    public static Reference31Validator getInstance() {
        return INSTANCE;
    }

    public static final Map<String, Function<Components, Map<String, ?>>> COMPONENT_GETTERS;
    static {
        COMPONENT_GETTERS = Map.of("callbacks", t -> t.getCallbacks(),
                                   "links", t -> t.getLinks(),
                                   "securitySchemes", t -> t.getSecuritySchemes(),
                                   "headers", t -> t.getHeaders(),
                                   "requestBodies", t -> t.getRequestBodies(),
                                   "examples", t -> t.getExamples(),
                                   "parameters", t -> t.getParameters(),
                                   "responses", t -> t.getResponses(),
                                   "schemas", t -> t.getSchemas(),
                                   "pathItems", t -> t.getPathItems());
    }

    private Reference31Validator() {}

    @FFDCIgnore(URISyntaxException.class)
    public Object validate(ValidationHelper helper, Context context, String ref) {
        // Check for a null or empty reference
        if (ref == null || ref.trim().isEmpty()) {
            final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NULL);
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            return null;
        }

        try {
            new URI(ref); // Parse as a URI
        } catch (URISyntaxException e) {
            final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_VALID_URI, ref);
            helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
            return null;
        }

        String[] references = ref.split("/");

        // Only validate references of the format #/components/<type>/<name>
        if (!ref.startsWith("#/components/")) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Reference does not target components, not validating", ref);
            }
            return null;
        }

        // If we don't have a components object, it can't be valid
        Components components = context.getModel().getComponents();
        if (components == null) {
            final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_PART_OF_MODEL, ref);
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            return null;
        }

        // Extract from #/components/<type>/<name>
        String type = references[2];

        // If type is not one of the known names, we don't validate it
        // It could be under an extension
        if (!COMPONENT_GETTERS.containsKey(type)) {
            if (type.startsWith("x-")) {
                // Reference to something in an extension, don't validate
                return null;
            } else {
                // Bad type name, can't exist
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_PART_OF_MODEL, ref);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                return null;
            }
        }

        Optional<Map<String, ?>> component = Optional.ofNullable(COMPONENT_GETTERS.get(type)) // Find the getter for the type
                                                     .map(f -> f.apply(components)); // Call the getter to get the map for the type

        if (references.length == 3) {
            // Direct reference to something under components. Return so we can validate that it's of the wrong type.
            if (component.isPresent()) {
                return component.get();
            } else {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_PART_OF_MODEL, ref);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                return null;
            }
        }

        // Extract from #/components/<type>/<name>
        String name = decodeJsonPointerToken(references[3]);

        // Look up the referenced object
        Optional<?> object = component.map(m -> m.get(name)); // Look for the entry within the map

        // Check if the referenced object is present
        if (!object.isPresent()) {
            final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_PART_OF_MODEL, ref);
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            return null;
        }

        // We found the referenced object, return it
        return object.get();
    }

    /**
     * Decode single part of a reference, according to https://www.rfc-editor.org/rfc/rfc6901#section-4
     *
     * @param token the encoded token
     * @return the decoded token
     */
    private String decodeJsonPointerToken(String token) {
        token = token.replaceAll("~1", "/");
        token = token.replaceAll("~0", "~");
        return token;
    }
}
