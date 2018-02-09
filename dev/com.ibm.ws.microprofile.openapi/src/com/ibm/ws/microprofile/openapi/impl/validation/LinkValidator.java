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

import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.links.Link;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class LinkValidator extends TypeValidator<Link> {

    private static final TraceComponent tc = Tr.register(LinkValidator.class);

    private static final LinkValidator INSTANCE = new LinkValidator();

    public static LinkValidator getInstance() {
        return INSTANCE;
    }

    private LinkValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Link t) {

        if (t != null) {

            String reference = t.getRef();

            if (reference != null && !reference.isEmpty()) {
                ValidatorUtils.referenceValidatorHelper(reference, t, helper, context, key);
                return;
            }
          
            Boolean operationRefDefined = t.getOperationRef() != null && !t.getOperationRef().isEmpty();
            Boolean operationIdDefined = t.getOperationId() != null && !t.getOperationId().isEmpty();
            Boolean operationRefNull = t.getOperationRef() == null || t.getOperationRef().isEmpty();
            Boolean operationIdNull = t.getOperationId() == null || t.getOperationId().isEmpty();

            if (operationRefDefined && operationIdDefined) {
                final String message = Tr.formatMessage(tc, "linkOperationRefOrId", t.toString());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }

            if (operationRefDefined || operationIdDefined) {
                if (operationRefDefined) {
                    Boolean isValid = true;
                    if (t.getOperationRef().startsWith("#")) {
                        String[] operationRef = t.getOperationRef().split("/");
                        if (operationRef.length != 4) {
                            isValid = false;
                        } else {
                            String pathKey = operationRef[2].replace("~1", "/").replace("~0", "~");
                            Paths paths = context.getModel().getPaths();

                            if (paths.containsKey(pathKey) && paths.get(pathKey) != null) {
                                String op = operationRef[3];
                                switch (op) {
                                    case "get":
                                        if (paths.get(pathKey).getGET() == null) {
                                            isValid = false;
                                        }
                                        break;
                                    case "put":
                                        if (paths.get(pathKey).getPUT() == null) {
                                            isValid = false;
                                        }
                                        break;
                                    case "post":
                                        if (paths.get(pathKey).getPOST() == null) {
                                            isValid = false;
                                        }
                                        break;
                                    case "delete":
                                        if (paths.get(pathKey).getDELETE() == null) {
                                            isValid = false;
                                        }
                                        break;
                                    case "trace":
                                        if (paths.get(pathKey).getTRACE() == null) {
                                            isValid = false;
                                        }
                                        break;
                                    case "head":
                                        if (paths.get(pathKey).getHEAD() == null) {
                                            isValid = false;
                                        }
                                        break;
                                    case "patch":
                                        if (paths.get(pathKey).getPATCH() == null) {
                                            isValid = false;
                                        }
                                        break;
                                    case "options":
                                        if (paths.get(pathKey).getOPTIONS() == null) {
                                            isValid = false;
                                        }
                                        break;
                                    default:
                                        isValid = false;
                                        break;
                                }
                            } else {
                                isValid = false;
                            }
                        }
                        if (!isValid) {
                            final String message = Tr.formatMessage(tc, "linkOperationRefInvalidOrMissing", t.getOperationRef());
                            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                        }
                    }
                }
                if (operationIdDefined) {
                    helper.addLinkOperationId(t.getOperationId(), context.getLocation());
                }
            } else if (operationRefNull && operationIdNull) {
                final String message = Tr.formatMessage(tc, "linkMustSpecifyOperationRefOrId", t.toString());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }
        }
    }
}
