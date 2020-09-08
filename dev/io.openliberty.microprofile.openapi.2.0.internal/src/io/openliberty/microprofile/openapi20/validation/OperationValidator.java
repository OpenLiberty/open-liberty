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

import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent.Severity;
import io.smallrye.openapi.runtime.io.operation.OperationConstant;

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
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.OPERATION_IDS_MUST_BE_UNIQUE, id);
                helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(OperationConstant.PROP_OPERATION_ID), message));
            }
            final APIResponses responses = t.getResponses();
            ValidatorUtils.validateRequiredField(responses, context, OperationConstant.PROP_RESPONSES).ifPresent(helper::addValidationEvent);
        }
    }
}
