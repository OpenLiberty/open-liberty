/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.impl.validation;

import java.util.ArrayList;
import java.util.List;
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

            List<Map<String, ?>> components = new ArrayList<Map<String, ?>>();

            if (t.getCallbacks() != null && !t.getCallbacks().isEmpty()) {
                components.add(t.getCallbacks());
            }
            if (t.getExamples() != null && !t.getExamples().isEmpty()) {
                components.add(t.getExamples());
            }
            if (t.getHeaders() != null && !t.getHeaders().isEmpty()) {
                components.add(t.getHeaders());
            }
            if (t.getLinks() != null && !t.getLinks().isEmpty()) {
                components.add(t.getLinks());
            }
            if (t.getParameters() != null && !t.getParameters().isEmpty()) {
                components.add(t.getParameters());
            }
            if (t.getRequestBodies() != null && !t.getRequestBodies().isEmpty()) {
                components.add(t.getRequestBodies());
            }
            if (t.getResponses() != null && !t.getResponses().isEmpty()) {
                components.add(t.getResponses());
            }
            if (t.getSchemas() != null && !t.getSchemas().isEmpty()) {
                components.add(t.getSchemas());
            }
            if (t.getSecuritySchemes() != null && !t.getSecuritySchemes().isEmpty()) {
                components.add(t.getSecuritySchemes());
            }
            if (!components.isEmpty()) {
                for (Map<String, ?> component : components) {
                    for (String k : component.keySet()) {
                        if (!k.matches("^[a-zA-Z0-9\\.\\-_]+$")) {
                            final String message = Tr.formatMessage(tc, "keyNotARegex", k, context.getLocation());
                            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                        }
                    }
                }
            }
        }
    }
}
