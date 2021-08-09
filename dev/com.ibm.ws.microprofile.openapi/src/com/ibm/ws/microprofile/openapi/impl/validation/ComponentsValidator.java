/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.impl.validation;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.Components;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class ComponentsValidator extends TypeValidator<Components> {

    private static final TraceComponent tc = Tr.register(ComponentsValidator.class);

    private static final ComponentsValidator INSTANCE = new ComponentsValidator();

    public static ComponentsValidator getInstance() {
        return INSTANCE;
    }

    private ComponentsValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Components t) {

        if (t != null) {
            Map<String, Map<String, ?>> components = new HashMap<String, Map<String, ?>>();
            if (t.getCallbacks() != null && !t.getCallbacks().isEmpty()) {
                components.put("callbacks", t.getCallbacks());
            }
            if (t.getExamples() != null && !t.getExamples().isEmpty()) {
                components.put("examples", t.getExamples());
            }
            if (t.getHeaders() != null && !t.getHeaders().isEmpty()) {
                components.put("headers", t.getHeaders());
            }
            if (t.getLinks() != null && !t.getLinks().isEmpty()) {
                components.put("links", t.getLinks());
            }
            if (t.getParameters() != null && !t.getParameters().isEmpty()) {
                components.put("parameters", t.getParameters());
            }
            if (t.getRequestBodies() != null && !t.getRequestBodies().isEmpty()) {
                components.put("requestBodies", t.getRequestBodies());
            }
            if (t.getResponses() != null && !t.getResponses().isEmpty()) {
                components.put("responses", t.getResponses());
            }
            if (t.getSchemas() != null && !t.getSchemas().isEmpty()) {
                components.put("schemas", t.getSchemas());
            }
            if (t.getSecuritySchemes() != null && !t.getSecuritySchemes().isEmpty()) {
                components.put("securitySchemes", t.getSecuritySchemes());
            }

            if (!components.isEmpty()) {
                for (String mapName : components.keySet()) {
                    Map<String, ?> component = components.get(mapName);
                    boolean mapContainsInvalidKey = false;
                    for (String k : component.keySet()) {
                        if (k != null) {
                            if (!k.matches("^[a-zA-Z0-9\\.\\-_]+$")) {
                                final String message = Tr.formatMessage(tc, "keyNotARegex", k);
                                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(mapName), message));
                            }

                            //Ensure map doesn't contain null value
                            if (component.get(k) == null) {
                                final String message = Tr.formatMessage(tc, "nullValueInMap", k);
                                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(mapName), message));
                            }
                        } else {
                            mapContainsInvalidKey = true;
                        }
                    }

                    //Ensure map doesn't contain an invalid key
                    if (mapContainsInvalidKey) {
                        final String message = Tr.formatMessage(tc, "nullOrEmptyKeyInMap");
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(mapName), message));
                    }
                }
            }
        }
    }
}
