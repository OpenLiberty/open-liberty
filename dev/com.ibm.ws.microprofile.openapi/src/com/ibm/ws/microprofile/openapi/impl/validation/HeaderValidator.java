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

import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class HeaderValidator extends TypeValidator<Header> {

    private static final TraceComponent tc = Tr.register(HeaderValidator.class);

    private static final HeaderValidator INSTANCE = new HeaderValidator();

    public static HeaderValidator getInstance() {
        return INSTANCE;
    }

    private HeaderValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Header t) {

        if (t != null) {

            String reference = t.getRef();

            if (reference != null && !reference.isEmpty()) {
                ValidatorUtils.referenceValidatorHelper(reference, t, helper, context, key);
                return;
            }

            // The examples object is mutually exclusive of the example object.
            if ((t.getExample() != null) && (t.getExamples() != null && !t.getExamples().isEmpty())) {
                final String message = Tr.formatMessage(tc, "headerExampleOrExamples", t);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            Schema schema = t.getSchema();
            Content content = t.getContent();
            // A parameter MUST contain either a schema property, or a content property, but not both.
            if (schema == null && content == null) {
                final String message = Tr.formatMessage(tc, "headerSchemaOrContent", t);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            if (schema != null && content != null) {
                final String message = Tr.formatMessage(tc, "headerSchemaAndContent", t);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));

            }

            //The 'content' map MUST only contain one entry.
            if (content != null && content.size() > 1) {
                final String message = Tr.formatMessage(tc, "headerContentMap", t);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));

            }
        }
    }
}
