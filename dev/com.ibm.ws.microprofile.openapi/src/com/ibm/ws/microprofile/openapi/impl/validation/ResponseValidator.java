/*
* IBM Confidential
*
* OCO Source Materials
*
* Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.microprofile.openapi.impl.validation;

import org.eclipse.microprofile.openapi.models.responses.APIResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class ResponseValidator extends TypeValidator<APIResponse> {

    private static final TraceComponent tc = Tr.register(ResponseValidator.class);

    private static final ResponseValidator INSTANCE = new ResponseValidator();

    public static ResponseValidator getInstance() {
        return INSTANCE;
    }

    private ResponseValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, APIResponse t) {

        String reference = t.getRef();

        if (reference != null && !reference.isEmpty()) {
            ValidatorUtils.referenceValidatorHelper(reference, t, helper, context, key);
            return;
        }

        final String description = t.getDescription();
        ValidatorUtils.validateRequiredField(description, context, "description").ifPresent(helper::addValidationEvent);

    }
}
