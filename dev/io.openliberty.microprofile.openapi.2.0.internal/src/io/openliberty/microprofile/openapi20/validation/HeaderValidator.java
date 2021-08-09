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

import java.util.Map;

import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;

/**
 *
 */
public class HeaderValidator extends TypeValidator<Header> {

    private static final TraceComponent tc = Tr.register(HeaderValidator.class);

    private static final HeaderValidator INSTANCE = new HeaderValidator();

    private HeaderValidator() {}

    public static HeaderValidator getInstance() {
        return INSTANCE;
    }

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
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.HEADER_EXAMPLE_OR_EXAMPLES, key);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }

            Schema schema = t.getSchema();
            Content content = t.getContent();
            // A parameter MUST contain either a schema property, or a content property, but not both.
            if (schema == null && content == null) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.HEADER_SCHEMA_OR_CONTENT, key);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            if (schema != null && content != null) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.HEADER_SCHEMA_AND_CONTENT, key);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            // The 'content' map MUST only contain one entry.
            if (content != null) {
                Map<String, MediaType> mediaTypes = content.getMediaTypes();
                if (mediaTypes != null && mediaTypes.size() > 1) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.HEADER_CONTENT_MAP, key);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                }
            }
        }
    }
}
