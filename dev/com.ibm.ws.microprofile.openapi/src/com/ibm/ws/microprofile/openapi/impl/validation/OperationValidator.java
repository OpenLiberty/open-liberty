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

import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent.Severity;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class OperationValidator extends TypeValidator<Operation> {

    private static final TraceComponent tc = Tr.register(OperationValidator.class);

    private static final OperationValidator INSTANCE = new OperationValidator();

    public static OperationValidator getInstance() {
        return INSTANCE;
    }

    private OperationValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Operation t) {
        if (t != null) {
            final String id = t.getOperationId();
            if (id != null && helper.addOperationId(id)) {
                final String message = Tr.formatMessage(tc, "operationIdsMustBeUnique", id);
                helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation("operationId"), message));
            }
            final APIResponses responses = t.getResponses();
            ValidatorUtils.validateRequiredField(responses, context, "responses").ifPresent(helper::addValidationEvent);
        }
    }
}
