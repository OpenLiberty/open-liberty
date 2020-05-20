/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.validation;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.Components;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.Constants;
import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;
import io.smallrye.openapi.runtime.io.components.ComponentsConstant;

/**
 *
 */
public class ComponentsValidator extends TypeValidator<Components> {

    private static final TraceComponent tc = Tr.register(ComponentsValidator.class);

    private static final ComponentsValidator INSTANCE = new ComponentsValidator();

    public static ComponentsValidator getInstance() {
        return INSTANCE;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Components t) {

        if (t != null) {
            Map<String, Map<String, ?>> components = new HashMap<String, Map<String, ?>>();
            if (t.getCallbacks() != null && !t.getCallbacks().isEmpty()) {
                components.put(ComponentsConstant.PROP_CALLBACKS, t.getCallbacks());
            }
            if (t.getExamples() != null && !t.getExamples().isEmpty()) {
                components.put(ComponentsConstant.PROP_EXAMPLES, t.getExamples());
            }
            if (t.getHeaders() != null && !t.getHeaders().isEmpty()) {
                components.put(ComponentsConstant.PROP_HEADERS, t.getHeaders());
            }
            if (t.getLinks() != null && !t.getLinks().isEmpty()) {
                components.put(ComponentsConstant.PROP_LINKS, t.getLinks());
            }
            if (t.getParameters() != null && !t.getParameters().isEmpty()) {
                components.put(ComponentsConstant.PROP_PARAMETERS, t.getParameters());
            }
            if (t.getRequestBodies() != null && !t.getRequestBodies().isEmpty()) {
                components.put(ComponentsConstant.PROP_REQUEST_BODIES, t.getRequestBodies());
            }
            if (t.getResponses() != null && !t.getResponses().isEmpty()) {
                components.put(ComponentsConstant.PROP_RESPONSES, t.getResponses());
            }
            if (t.getSchemas() != null && !t.getSchemas().isEmpty()) {
                components.put(ComponentsConstant.PROP_SCHEMAS, t.getSchemas());
            }
            if (t.getSecuritySchemes() != null && !t.getSecuritySchemes().isEmpty()) {
                components.put(ComponentsConstant.PROP_SECURITY_SCHEMES, t.getSecuritySchemes());
            }

            if (!components.isEmpty()) {
                for (String mapName : components.keySet()) {
                    Map<String, ?> component = components.get(mapName);
                    boolean mapContainsInvalidKey = false;
                    for (String k : component.keySet()) {
                        if (k != null) {
                            if (!Constants.REGEX_COMPONENT_KEY_PATTERN.matcher(k).matches()) {
                                final String message = Tr.formatMessage(tc, ValidationMessageConstants.KEY_NOT_A_REGEX, k);
                                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(mapName), message));
                            }

                            //Ensure map doesn't contain null value
                            if (component.get(k) == null) {
                                final String message = Tr.formatMessage(tc, ValidationMessageConstants.NULL_VALUE_IN_MAP, k);
                                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(mapName), message));
                            }
                        } else {
                            mapContainsInvalidKey = true;
                        }
                    } // FOR

                    //Ensure map doesn't contain an invalid key
                    if (mapContainsInvalidKey) {
                        final String message = Tr.formatMessage(tc, ValidationMessageConstants.NULL_OR_EMPTY_KEY_IN_MAP);
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(mapName), message));
                    }
                }
            }
        }
    }
    
    private ComponentsValidator() {
        // This class is not meant to be instantiated.
    }
}
