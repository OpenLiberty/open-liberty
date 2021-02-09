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

import org.eclipse.microprofile.openapi.models.parameters.RequestBody;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.smallrye.openapi.runtime.io.requestbody.RequestBodyConstant;

/**
 *
 */
public class RequestBodyValidator extends TypeValidator<RequestBody> {

    private static final TraceComponent tc = Tr.register(RequestBodyValidator.class);

    private static final RequestBodyValidator INSTANCE = new RequestBodyValidator();

    public static RequestBodyValidator getInstance() {
        return INSTANCE;
    }

    private RequestBodyValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, RequestBody t) {

        String reference = t.getRef();

        if (reference != null && !reference.isEmpty()) {
            ValidatorUtils.referenceValidatorHelper(reference, t, helper, context, key);
            return;
        }
        ValidatorUtils.validateRequiredField(t.getContent(), context, RequestBodyConstant.PROP_CONTENT).ifPresent(helper::addValidationEvent);

    }
}
