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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.internal.validation.OASValidationResult.ValidationEvent;

/**
 *
 */
public class ExtensionValidator extends TypeValidator<Object> {

    private static final TraceComponent tc = Tr.register(ExtensionValidator.class);

    private static final ExtensionValidator INSTANCE = new ExtensionValidator();

    public static ExtensionValidator getInstance() {
        return INSTANCE;
    }

    private ExtensionValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Object t) {

        if (key != null) {
            if (!key.startsWith("x-")) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.INVALID_EXTENSION_NAME, key);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }
        }
    }
}
