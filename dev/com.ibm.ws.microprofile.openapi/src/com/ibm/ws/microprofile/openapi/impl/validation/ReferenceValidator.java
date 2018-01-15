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

import java.util.Map;
import java.util.logging.Logger;

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
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class ReferenceValidator {

    private static final TraceComponent tc = Tr.register(ReferenceValidator.class);

    private static final Logger LOGGER = Logger.getLogger(ReferenceValidator.class.getName());

    private static final ReferenceValidator INSTANCE = new ReferenceValidator();

    public static ReferenceValidator getInstance() {
        return INSTANCE;
    }

    private ReferenceValidator() {}

    /** {@inheritDoc} */

    public Object validate(ValidationHelper helper, Context context, String key, String $ref) {

        if (context.getModel().getComponents() != null && $ref != null) {
            //Store model components in a variable
            Components components = context.getModel().getComponents();
            if ($ref.contains(".json")
                || $ref.contains(".yml")
                || $ref.contains(".yaml")
                || $ref.contains("/x-")
                || $ref.startsWith("http://")
                || $ref.startsWith("https://")) {

                final String message = Tr.formatMessage(tc, "referenceExternalOrExtension", $ref);
                LOGGER.warning(message);

            } else {

                boolean validRefStruct = true;

                String[] references = $ref.split("/");
                //Validate that $ref starts with # and has no duplicates in the path - i.e. the format of an internal reference string is correct
                if (!($ref.startsWith("#/components/") && references.length == 4)) {
                    validRefStruct = false;
                }

                //If ref does not contain any duplicates, and the first two elements are # and components, then the length of a valid ref array should be 4 elements, i.e. #/components/examples/MyExample
                if (validRefStruct) {
                    //Start at index 2 because index 0 is # and index 1 is components
                    for (int i = 2; i < ((references.length) - 1); i++) {
                        String name = references[i + 1];

                        switch (references[i]) {
                            case "schemas":

                                Map<String, Schema> schemas = components.getSchemas();

                                if (schemas != null && schemas.containsKey(name)) {
                                    return schemas.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, "referenceNotPartOfModel", $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
                                    break;
                                }

                            case "responses":

                                Map<String, APIResponse> responses = components.getResponses();

                                if (responses != null && responses.containsKey(name)) {
                                    return responses.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, "referenceNotPartOfModel", $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
                                    break;
                                }

                            case "parameters":

                                Map<String, Parameter> parameters = components.getParameters();

                                if (parameters != null && parameters.containsKey(name)) {
                                    return parameters.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, "referenceNotPartOfModel", $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
                                    break;
                                }

                            case "examples":

                                Map<String, Example> examples = components.getExamples();

                                if (examples != null && examples.containsKey(name)) {
                                    return examples.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, "referenceNotPartOfModel", $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
                                    break;
                                }

                            case "requestBodies":

                                Map<String, RequestBody> requestBodies = components.getRequestBodies();

                                if (requestBodies != null && requestBodies.containsKey(name)) {
                                    return requestBodies.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, "referenceNotPartOfModel", $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
                                    break;
                                }

                            case "headers":

                                Map<String, Header> headers = components.getHeaders();

                                if (headers != null && headers.containsKey(name)) {
                                    return headers.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, "referenceNotPartOfModel", $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
                                    break;
                                }

                            case "securitySchemes":

                                Map<String, SecurityScheme> securitySchemes = components.getSecuritySchemes();

                                if (securitySchemes != null && securitySchemes.containsKey(name)) {
                                    return securitySchemes.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, "referenceNotPartOfModel", $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
                                    break;
                                }

                            case "links":

                                Map<String, Link> links = components.getLinks();

                                if (links != null && links.containsKey(name)) {
                                    return links.get(name);
                                } else {
                                    final String message = Tr.formatMessage(tc, "referenceNotPartOfModel", $ref);
                                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
                                    break;
                                }

                            default:
                                final String message = Tr.formatMessage(tc, "referenceNotValid", $ref);
                                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
                                break;
                        }
                    }
                } else {
                    final String message = Tr.formatMessage(tc, "referenceNotValidFormat", $ref);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
                }
            }

        } else {
            final String message = Tr.formatMessage(tc, "referenceNull", context.getModel().getComponents(), $ref);
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
        }

        return null;
    }

}
