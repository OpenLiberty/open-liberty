/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal.validation;

import java.util.Map;

import javax.ws.rs.core.Response.Status.Family;

import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.microprofile.openapi20.internal.utils.Constants;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.internal.validation.OASValidationResult.ValidationEvent;
import io.openliberty.microprofile.openapi20.internal.validation.OASValidationResult.ValidationEvent.Severity;
import io.smallrye.openapi.runtime.io.response.ResponseConstant;

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
            Map<String, APIResponse> responses = t.getAPIResponses();
            if (responses != null) {
                if (responses.size() == 0 && t.getDefaultValue() == null) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.RESPONSE_MUST_CONTAIN_ONE_CODE);
                    helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
                } else if (!responses.keySet().stream().anyMatch(this::isSuccessStatusCode)) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.RESPONSE_SHOULD_CONTAIN_SUCCESS);
                    helper.addValidationEvent(new ValidationEvent(Severity.WARNING, context.getLocation(), message));
                }
                for (String k : responses.keySet()) {
                    // Ensure map doesn't contain null value
                    if (responses.get(k) == null) {
                        final String message = Tr.formatMessage(tc, ValidationMessageConstants.NULL_VALUE_IN_MAP, k);
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                    }
                } // FOR
            } else { // IF - responses != null
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.RESPONSE_MUST_CONTAIN_ONE_CODE);
                helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
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
        if (Constants.RESPONSE_RANGE_SUCCESS.equals(s) || ResponseConstant.PROP_DEFAULT.equals(s)) {
            return true;
        }
        if (s.length() != 3) {
            return false;
        }
        try {
            return Family.familyOf(Integer.parseInt(s)) == Family.SUCCESSFUL;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }
}
