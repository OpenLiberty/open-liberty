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

import org.eclipse.microprofile.openapi.models.responses.APIResponses;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent.Severity;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class ResponsesValidator extends TypeValidator<APIResponses> {

    private static final TraceComponent tc = Tr.register(ResponsesValidator.class);

    private static final ResponsesValidator INSTANCE = new ResponsesValidator();

    public static ResponsesValidator getInstance() {
        return INSTANCE;
    }

    private ResponsesValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, APIResponses t) {
        if (t != null) {
            if (t.size() == 0 && t.getDefault() == null) {
                final String message = Tr.formatMessage(tc, "responseMustContainOneCode");
                helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
            } else if (!t.keySet().stream().anyMatch(v -> isSuccessStatusCode(v))) {
                final String message = Tr.formatMessage(tc, "responseShouldContainSuccess");
                helper.addValidationEvent(new ValidationEvent(Severity.WARNING, context.getLocation(), message));
            }
        }
    }

    /*
     * Returns true if s = [200-299] or 2XX
     */
    @FFDCIgnore(NumberFormatException.class)
    private boolean isSuccessStatusCode(String s) {
        if (s == null) {
            return false;
        }
        if ("2XX".equals(s) || "default".equals(s)) {
            return true;
        }
        if (s.length() != 3) {
            return false;
        }
        try {
            final int i = Integer.parseInt(s);
            return i >= 200 && i <= 299;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
