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

import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;
import io.smallrye.openapi.runtime.io.Parameterizable;
import io.smallrye.openapi.runtime.io.parameter.ParameterConstant;

/**
 *
 */
public class ParameterValidator extends TypeValidator<Parameter> {

    private static final TraceComponent tc = Tr.register(ParameterValidator.class);

    private static final ParameterValidator INSTANCE = new ParameterValidator();

    public static ParameterValidator getInstance() {
        return INSTANCE;
    }

    private ParameterValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Parameter t) {

        if (t != null) {

            String reference = t.getRef();

            if (reference != null && !reference.isEmpty()) {
                ValidatorUtils.referenceValidatorHelper(reference, t, helper, context, key);
                return;
            }

            ValidatorUtils.validateRequiredField(t.getName(), context, Parameterizable.PROP_NAME).ifPresent(helper::addValidationEvent);

            In in = t.getIn();
            ValidatorUtils.validateRequiredField(in, context, ParameterConstant.PROP_IN).ifPresent(helper::addValidationEvent);

            if (in != null && in != In.COOKIE && in != In.HEADER && in != In.PATH && in != In.QUERY) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.PARAMETER_IN_FIELD_INVALID, in, t.getName());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(ParameterConstant.PROP_IN), message));
            }

            // The examples object is mutually exclusive of the example object.
            if ((t.getExample() != null) && (t.getExamples() != null && !t.getExamples().isEmpty())) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.PARAMETER_EXAMPLE_OR_EXAMPLES, t.getName());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }

            Schema schema = t.getSchema();
            Content content = t.getContent();
            // A parameter MUST contain either a schema property, or a content property, but not both.
            if (schema == null && content == null) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.PARAMETER_SCHEMA_OR_CONTENT, t.getName());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }
            if (schema != null && content != null) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.PARAMETER_SCHEMA_AND_CONTENT, t.getName());
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            // The 'content' map MUST only contain one entry.
            if (content != null) {
                Map<String, MediaType> mediaTypes = content.getMediaTypes();
                if (mediaTypes != null && mediaTypes.size() > 1) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.PARAMETER_CONTENT_MAP_MUST_NOT_BE_EMPTY, t.getName());
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(Parameterizable.PROP_CONTENT), message));
                }
            }
        }
    }
}
