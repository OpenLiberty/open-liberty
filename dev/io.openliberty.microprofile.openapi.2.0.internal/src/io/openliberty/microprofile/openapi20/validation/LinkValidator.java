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

import javax.ws.rs.HttpMethod;

import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.links.Link;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;

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

            boolean operationRefDefined = t.getOperationRef() != null && !t.getOperationRef().isEmpty();
            boolean operationIdDefined = t.getOperationId() != null && !t.getOperationId().isEmpty();

            if (operationRefDefined && operationIdDefined) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.LINK_OPERATION_REF_AND_ID, key);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }

            if (!operationRefDefined && !operationIdDefined) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.LINK_MUST_SPECIFY_OPERATION_REF_OR_ID, key);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            } else {
                if (operationIdDefined) {
                    helper.addLinkOperationId(t.getOperationId(), context.getLocation());
                }

                if (operationRefDefined) {
                    if (t.getOperationRef().startsWith("#")) {
                        boolean isValid = true;
                        String[] operationRef;
                        if (!t.getOperationRef().startsWith("#/paths/") || (operationRef = t.getOperationRef().split("/")).length != 4) {
                            isValid = false;
                        } else {
                            String pathKey = operationRef[2].replace("~1", "/").replace("~0", "~");
                            Paths paths = context.getModel().getPaths();

                            if (paths != null && paths.getPathItem(pathKey) != null) {
                                String op = operationRef[3].toUpperCase();
                                switch (op) {
                                    case HttpMethod.GET:
                                        if (paths.getPathItem(pathKey).getGET() == null) {
                                            isValid = false;
                                        }
                                        break;
                                    case HttpMethod.PUT:
                                        if (paths.getPathItem(pathKey).getPUT() == null) {
                                            isValid = false;
                                        }
                                        break;
                                    case HttpMethod.POST:
                                        if (paths.getPathItem(pathKey).getPOST() == null) {
                                            isValid = false;
                                        }
                                        break;
                                    case HttpMethod.DELETE:
                                        if (paths.getPathItem(pathKey).getDELETE() == null) {
                                            isValid = false;
                                        }
                                        break;
                                    case HttpMethod.OPTIONS:
                                        if (paths.getPathItem(pathKey).getOPTIONS() == null) {
                                            isValid = false;
                                        }
                                        break;
                                    case HttpMethod.HEAD:
                                        if (paths.getPathItem(pathKey).getHEAD() == null) {
                                            isValid = false;
                                        }
                                        break;
                                    case HttpMethod.PATCH:
                                        if (paths.getPathItem(pathKey).getPATCH() == null) {
                                            isValid = false;
                                        }
                                        break;
                                    case "TRACE":
                                        if (paths.getPathItem(pathKey).getTRACE() == null) {
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
                            final String message = Tr.formatMessage(tc, ValidationMessageConstants.LINK_OPERATION_REF_INVALID_OR_MISSING, key, t.getOperationRef());
                            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                        }
                    }
                }
            }
        }
    }
}
