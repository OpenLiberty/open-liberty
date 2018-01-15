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

import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

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

        String reference = t.getRef();

        if (reference != null && !reference.isEmpty()) {
            ValidatorUtils.referenceValidatorHelper(reference, t, helper, context, key);
            return;
        }

        ValidatorUtils.validateRequiredField(t.getName(), "parameter", "name").ifPresent(helper::addValidationEvent);

        In in = t.getIn();
        ValidatorUtils.validateRequiredField(in, "parameter." + t.getName(), "in").ifPresent(helper::addValidationEvent);

        if (in != null) {
            final String message = Tr.formatMessage(tc, "parameterInFieldInvalid", t.getName(), in);
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
        }

        // The examples object is mutually exclusive of the example object.
        if ((t.getExample() != null) && (t.getExamples() != null && !t.getExamples().isEmpty())) {
            final String message = Tr.formatMessage(tc, "parameterExampleOrExamples", t.getName());
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, null, message));
        }

        Schema schema = t.getSchema();
        Content content = t.getContent();
        // A parameter MUST contain either a schema property, or a content property, but not both.
        if (schema == null && content == null) {
            final String message = Tr.formatMessage(tc, "parameterSchemaOrContent", t.getName());
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
        }
        if (schema != null && content != null) {
            final String message = Tr.formatMessage(tc, "parameterSchemaAndContent", t.getName());
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
        }

        // The 'content' map MUST only contain one entry.
        if (content != null && content.size() > 1) {
            final String message = Tr.formatMessage(tc, "parameterContentMapMustNotBeEmpty", t.getName());
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
        }
    }
}
