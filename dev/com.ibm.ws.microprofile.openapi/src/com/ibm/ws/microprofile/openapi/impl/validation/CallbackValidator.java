/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.impl.validation;

import java.util.List;

import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

/**
 *
 */
public class CallbackValidator extends TypeValidator<Callback> {

    private static final TraceComponent tc = Tr.register(CallbackValidator.class);

    private static final CallbackValidator INSTANCE = new CallbackValidator();

    public static CallbackValidator getInstance() {
        return INSTANCE;
    }

    private CallbackValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Callback t) {
        String message;
        for (String urlTemplate : t.keySet()) {
            // validate urlTemplate is valid
            if (urlTemplate.isEmpty()) {
                message = Tr.formatMessage(tc, "callbackURLTemplateEmpty");
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                continue;
            }

            List<String> vars = RuntimeExpressionUtils.extractURLVars(urlTemplate);
            if (vars == null) {
                message = Tr.formatMessage(tc, "callbackInvalidSubstitutionVariables", urlTemplate);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            } else {
                // validate replacement items
                for (String v : vars) {
                    if (!RuntimeExpressionUtils.isRuntimeExpression(v)) {
                        message = Tr.formatMessage(tc, "callbackMustBeRuntimeExpression", v);
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                    }
                }
                // replace template fields with sample data
                String buildURL = urlTemplate;
                for (String v : vars) {
                    String templateVar = "{" + v + "}";
                    buildURL = buildURL.replace(templateVar, "e"); //buildURL.replace(templateVar, "e"); // Sample data
                }

                if (urlTemplate.contains("{$")) {
                    //Path within a Callback can contain variables (e.g. {$request.query.callbackUrl}/data ) which shouldn't be validated since they are not path params
                    if (OpenAPIUtils.isDebugEnabled(tc)) {
                        Tr.debug(tc, "Path contains variables. Skip validation of url: " + key);
                    }
                } else {
                    // validate remaining url
                    //validating buildURL as URI to account for relative paths
                    if (!ValidatorUtils.isValidURI(buildURL)) {
                        message = Tr.formatMessage(tc, "callbackInvalidURL", urlTemplate);
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                    }
                }
            }
            // validate Path item
            Object pathItem = t.get(urlTemplate);
            if (!(pathItem instanceof PathItem)) {
                message = Tr.formatMessage(tc, "callbackInvalidPathItem", urlTemplate);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }
        }
    }
}
