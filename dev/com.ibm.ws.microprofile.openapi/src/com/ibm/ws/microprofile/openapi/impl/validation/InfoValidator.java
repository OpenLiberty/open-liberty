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

import org.eclipse.microprofile.openapi.models.info.Info;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class InfoValidator extends TypeValidator<Info> {

    private static final TraceComponent tc = Tr.register(InfoValidator.class);

    private static final InfoValidator INSTANCE = new InfoValidator();

    public static InfoValidator getInstance() {
        return INSTANCE;
    }

    private InfoValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Info t) {
        ValidatorUtils.validateRequiredField(t.getVersion(), context, "version").ifPresent(helper::addValidationEvent);
        ValidatorUtils.validateRequiredField(t.getTitle(), context, "title").ifPresent(helper::addValidationEvent);
        if (t.getTermsOfService() != null) {
            if (!ValidatorUtils.isValidURL(t.getTermsOfService())) {
                final String message = Tr.formatMessage(tc, "infoTermsOfServiceInvalidURL", t.getTermsOfService());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation("termsOfService"), message));
            }
        }
    }
}
