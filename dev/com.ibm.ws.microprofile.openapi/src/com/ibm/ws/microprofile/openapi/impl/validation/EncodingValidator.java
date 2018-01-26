/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

            if (t.getStyle() != null) {

                Style style = t.getStyle();
                Boolean explode = t.getExplode();
                String[] styleExplode = { "spaceDelimited", "pipeDelimited", "deepObject" };

                if (style == Encoding.Style.FORM && !explode) {

                    final String message = Tr.formatMessage(tc, "encodingExplodeForForm");
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));

                } else if (Arrays.asList(styleExplode).contains(style.toString()) && explode) {

                    final String message = Tr.formatMessage(tc, "encodingExplodeForOtherStyles");
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
                }
            }
        }
    }
}
