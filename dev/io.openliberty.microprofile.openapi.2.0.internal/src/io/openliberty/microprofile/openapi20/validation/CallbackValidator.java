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

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.LoggingUtils;
import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;

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
        Map<String, PathItem> pathItems = t.getPathItems();
        if (pathItems != null) {
            for (String urlTemplate : pathItems.keySet()) {
                // validate urlTemplate is valid
                if (urlTemplate.isEmpty()) {
                    message = Tr.formatMessage(tc, ValidationMessageConstants.CALLBACK_URL_TEMPLATE_EMPTY);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                    continue;
                }
                
                List<String> vars = RuntimeExpressionUtils.extractURLVars(urlTemplate);
                if (vars == null) {
                    message = Tr.formatMessage(tc, ValidationMessageConstants.CALLBACK_INVALID_SUBSTITUTION_VARIABLES, urlTemplate);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                } else {
                    // validate replacement items
                    for (String v : vars) {
                        if (!RuntimeExpressionUtils.isRuntimeExpression(v)) {
                            message = Tr.formatMessage(tc, ValidationMessageConstants.CALLBACK_MUST_BE_RUNTIME_EXPRESSION, v);
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
                        if (LoggingUtils.isDebugEnabled(tc)) {
                            Tr.debug(tc, "Path contains variables. Skip validation of url: " + key);
                        }
                    } else {
                        // validate remaining url
                        //validating buildURL as URI to account for relative paths
                        if (!ValidatorUtils.isValidURI(buildURL)) {
                            message = Tr.formatMessage(tc, ValidationMessageConstants.CALLBACK_INVALID_URL, urlTemplate);
                            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                        }
                    }
                }
                // Validate the PathItem
                Object pathItem = t.getPathItem(urlTemplate);
                if (!(pathItem instanceof PathItem)) {
                    message = Tr.formatMessage(tc, ValidationMessageConstants.CALLBACK_INVALID_PATH_ITEM, urlTemplate);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                }
            } // FOR
        }
    }
}
