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

import java.util.Arrays;

import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.Encoding.Style;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class EncodingValidator extends TypeValidator<Encoding> {

    private static final TraceComponent tc = Tr.register(EncodingValidator.class);

    private static final EncodingValidator INSTANCE = new EncodingValidator();

    public static EncodingValidator getInstance() {
        return INSTANCE;
    }

    private EncodingValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Encoding t) {
        if (t != null) {
            if (t.getContentType() != null) {

                String type = t.getContentType();
                String[] typeValues = { "application/octet-stream", "text/plain", "application/json", "image/*", "*, *", "" };

                if (!Arrays.asList(typeValues).contains(type)) {
                    final String message = Tr.formatMessage(tc, "encodingInvalidContentType");
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, null, message));
                }
            }

            if (t.getStyle() != null) {

                Style style = t.getStyle();
                Boolean explode = t.getExplode();
                String[] styleEnums = { "form", "spaceDelimited", "pipeDelimited", "deepObject", "" };
                String[] styleExplode = { "spaceDelimited", "pipeDelimited", "deepObject" };

                if (!Arrays.asList(styleEnums).contains(style)) {

                    final String message = Tr.formatMessage(tc, "encodingInvalidStyle");
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));

                } else {

                    if (style.equals("form") && !explode) {

                        final String message = Tr.formatMessage(tc, "encodingExplodeForForm");
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, null, message));

                    } else if (Arrays.asList(styleExplode).contains(style) && explode) {

                        final String message = Tr.formatMessage(tc, "encodingExplodeForOtherStyles");
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, null, message));

                    }
                }
            }

            else if (t.getStyle() == null && t.getExplode()) {

                final String message = Tr.formatMessage(tc, "encodingExplodeForNullStyle");
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, null, message));

            }

        } else {

            final String message = Tr.formatMessage(tc, "encodingObjectNull");
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));

        }
    }
}
