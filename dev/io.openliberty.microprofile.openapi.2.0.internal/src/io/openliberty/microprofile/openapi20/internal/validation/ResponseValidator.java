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

import org.eclipse.microprofile.openapi.models.responses.APIResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.smallrye.openapi.runtime.io.response.ResponseConstant;

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

        if (t != null) {
            String reference = t.getRef();

            if (reference != null && !reference.isEmpty()) {
                ValidatorUtils.referenceValidatorHelper(reference, t, helper, context, key);
                return;
            }

            final String description = t.getDescription();
            ValidatorUtils.validateRequiredField(description, context, ResponseConstant.PROP_DESCRIPTION).ifPresent(helper::addValidationEvent);
        }
    }
}
