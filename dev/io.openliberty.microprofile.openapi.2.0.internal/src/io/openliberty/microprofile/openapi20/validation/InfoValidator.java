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

import org.eclipse.microprofile.openapi.models.info.Info;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;
import io.smallrye.openapi.runtime.io.info.InfoConstant;

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
        if (t != null) {
            ValidatorUtils.validateRequiredField(t.getVersion(), context, InfoConstant.PROP_VERSION).ifPresent(helper::addValidationEvent);
            ValidatorUtils.validateRequiredField(t.getTitle(), context, InfoConstant.PROP_TITLE).ifPresent(helper::addValidationEvent);
            if (t.getTermsOfService() != null) {
                if (!ValidatorUtils.isValidURI(t.getTermsOfService())) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.INFO_TERMS_OF_SERVICE, t.getTermsOfService());
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(InfoConstant.PROP_TERMS_OF_SERVICE), message));
                }
            }
        }
    }
}
