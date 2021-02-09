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

import java.util.Map;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;
import io.smallrye.openapi.runtime.io.components.ComponentsConstant;

/**
 *
 */
public class ReferenceValidator {

    private static final TraceComponent tc = Tr.register(ReferenceValidator.class);

    private static final ReferenceValidator INSTANCE = new ReferenceValidator();

    public static ReferenceValidator getInstance() {
        return INSTANCE;
    }

    private ReferenceValidator() {}

    public Object validate(ValidationHelper helper, Context context, String key, String $ref) {
        if ($ref != null && !$ref.trim().isEmpty()) {
            //Validates relative references only (for now)
            if (!$ref.startsWith("#")) {
                return null;

            } else {

                boolean validRefStruct = true;

                String[] references = $ref.split("/");
                //Validate that $ref starts with # and has no duplicates in the path - i.e. the format of an internal reference string is correct
                if (!($ref.startsWith("#/components/") && references.length == 4)) {
                    validRefStruct = false;
                }

                //If ref does not contain any duplicates, and the first two elements are # and components, then the length of a valid ref array should be 4 elements, i.e. #/components/examples/MyExample
                if (validRefStruct) {
                    //Store model components in a variable
                    Components components = context.getModel().getComponents();

                    //If components is null then the reference can't be matched to a valid object.
                    if (components == null) {
                        final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_PART_OF_MODEL, $ref);
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                        return null;
                    }

                    //Start at index 2 because index 0 is # and index 1 is components
                    for (int i = 2; i < ((references.length) - 1); i++) {
                        String name = references[i + 1];
                        switch (references[i]) {
                            case ComponentsConstant.PROP_SCHEMAS:

                                Map<String, Schema> schemas = components.getSchemas();

                                if (schemas != null && schemas.containsKey(name)) {
                                    return schemas.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_PART_OF_MODEL, $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                                    break;
                                }

                            case ComponentsConstant.PROP_RESPONSES:

                                Map<String, APIResponse> responses = components.getResponses();

                                if (responses != null && responses.containsKey(name)) {
                                    return responses.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_PART_OF_MODEL, $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                                    break;
                                }

                            case ComponentsConstant.PROP_PARAMETERS:

                                Map<String, Parameter> parameters = components.getParameters();

                                if (parameters != null && parameters.containsKey(name)) {
                                    return parameters.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_PART_OF_MODEL, $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                                    break;
                                }

                            case ComponentsConstant.PROP_EXAMPLES:

                                Map<String, Example> examples = components.getExamples();

                                if (examples != null && examples.containsKey(name)) {
                                    return examples.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_PART_OF_MODEL, $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                                    break;
                                }

                            case ComponentsConstant.PROP_REQUEST_BODIES:

                                Map<String, RequestBody> requestBodies = components.getRequestBodies();

                                if (requestBodies != null && requestBodies.containsKey(name)) {
                                    return requestBodies.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_PART_OF_MODEL, $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                                    break;
                                }

                            case ComponentsConstant.PROP_HEADERS:

                                Map<String, Header> headers = components.getHeaders();

                                if (headers != null && headers.containsKey(name)) {
                                    return headers.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_PART_OF_MODEL, $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                                    break;
                                }

                            case ComponentsConstant.PROP_SECURITY_SCHEMES:

                                Map<String, SecurityScheme> securitySchemes = components.getSecuritySchemes();

                                if (securitySchemes != null && securitySchemes.containsKey(name)) {
                                    return securitySchemes.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_PART_OF_MODEL, $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                                    break;
                                }

                            case ComponentsConstant.PROP_LINKS:

                                Map<String, Link> links = components.getLinks();

                                if (links != null && links.containsKey(name)) {
                                    return links.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_PART_OF_MODEL, $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                                    break;
                                }

                            default:
                                final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_VALID, $ref);
                                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                                break;
                        }
                    }
                } else {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NOT_VALID_FORMAT, $ref);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                }
            }

        } else {
            final String message = Tr.formatMessage(tc, ValidationMessageConstants.REFERENCE_NULL);
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
        }

        return null;
    }
}
