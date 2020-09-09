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

import org.eclipse.microprofile.openapi.models.info.License;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;
import io.smallrye.openapi.runtime.io.license.LicenseConstant;

/**
 *
 */
public class LicenseValidator extends TypeValidator<License> {

    private static final TraceComponent tc = Tr.register(LicenseValidator.class);

    private static final LicenseValidator INSTANCE = new LicenseValidator();

    public static LicenseValidator getInstance() {
        return INSTANCE;
    }

    private LicenseValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, License t) {

        if (t != null) {
            ValidatorUtils.validateRequiredField(t.getName(), context, LicenseConstant.PROP_NAME).ifPresent(helper::addValidationEvent);

            if (t.getUrl() != null) {
                if (!ValidatorUtils.isValidURI(t.getUrl())) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.LICENSE_INVALID_URL, t.getUrl());
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                }
            }
        }
    }
}
