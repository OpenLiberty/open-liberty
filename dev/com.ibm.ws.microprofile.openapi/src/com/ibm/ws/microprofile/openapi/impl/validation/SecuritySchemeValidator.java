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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class SecuritySchemeValidator extends TypeValidator<SecurityScheme> {

    private static final TraceComponent tc = Tr.register(SecuritySchemeValidator.class);

    private static final SecuritySchemeValidator INSTANCE = new SecuritySchemeValidator();

    public static SecuritySchemeValidator getInstance() {
        return INSTANCE;
    }

    private SecuritySchemeValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, SecurityScheme t) {

        String reference = t.getRef();

        if (reference != null && !reference.isEmpty()) {
            ValidatorUtils.referenceValidatorHelper(reference, t, helper, context, key);
            return;
        }

        Optional<ValidationEvent> op_type = ValidatorUtils.validateRequiredField(t.getType(), context, "type");

        if (op_type.isPresent()) {
            op_type.ifPresent(helper::addValidationEvent);

        } else {
            String type = t.getType().toString();
            if ("apiKey".equals(type)) {
                ValidatorUtils.validateRequiredField(t.getName(), context, "name").ifPresent(helper::addValidationEvent);

                Optional<ValidationEvent> op_in = ValidatorUtils.validateRequiredField(t.getIn(), context, "in");

                if (op_in.isPresent()) {
                    op_in.ifPresent(helper::addValidationEvent);
                } else {
                    Set<String> inValues = new HashSet<String>(Arrays.asList("query", "header", "cookie"));
                    if (!(inValues.contains(t.getIn().toString()))) {
                        final String message = Tr.formatMessage(tc, "securitySchemeInFieldInvalid", key, t.getIn().toString());
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                    }

                }

            } else if ("http".equals(type)) {
                ValidatorUtils.validateRequiredField(t.getScheme(), context, "scheme").ifPresent(helper::addValidationEvent);

            } else if ("oauth2".equals(type)) {
                ValidatorUtils.validateRequiredField(t.getFlows(), context, "flows").ifPresent(helper::addValidationEvent);

            } else if ("openIdConnect".equals(type)) {
                Optional<ValidationEvent> op_url = ValidatorUtils.validateRequiredField(t.getOpenIdConnectUrl(), context, "openIdConnectUrl");

                if (op_url.isPresent()) {
                    op_url.ifPresent(helper::addValidationEvent);

                } else {
                    if (!(ValidatorUtils.isValidURL(t.getOpenIdConnectUrl()))) {
                        final String message = Tr.formatMessage(tc, "securitySchemeInvalidURL", t.getOpenIdConnectUrl());
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                    }
                }
            }

            //Issue warnings for non-applicable fields

            //'bearerFormat' field is only applicable to 'http' type
            if (t.getBearerFormat() != null && !t.getBearerFormat().isEmpty() && !"http".equals(type)) {
                final String message = Tr.formatMessage(tc, "nonApplicableFieldWithValue", "bearerFormat", t.getBearerFormat(), "Security Scheme Object", type);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }

            //'scheme' field is only applicable to 'http' type
            if (t.getScheme() != null && !t.getScheme().isEmpty() && !"http".equals(type)) {
                final String message = Tr.formatMessage(tc, "nonApplicableFieldWithValue", "scheme", t.getScheme(), "Security Scheme Object", type);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }

            //'in' field is only applicable to 'apiKey' type
            if (t.getIn() != null && !"apiKey".equals(type)) {
                final String message = Tr.formatMessage(tc, "nonApplicableFieldWithValue", "in", t.getIn(), "Security Scheme Object", type);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }

            //'name' field is only applicable to 'apiKey' type
            if (t.getName() != null && !t.getName().isEmpty() && !"apiKey".equals(type)) {
                final String message = Tr.formatMessage(tc, "nonApplicableFieldWithValue", "name", t.getName(), "Security Scheme Object", type);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }

            //'openIdConnectUrl' field is only applicable to 'openIdConnect' type
            if (t.getOpenIdConnectUrl() != null && !t.getOpenIdConnectUrl().isEmpty() && !"openIdConnect".equals(type)) {
                final String message = Tr.formatMessage(tc, "nonApplicableFieldWithValue", "openIdConnectUrl", t.getOpenIdConnectUrl(), "Security Scheme Object", type);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }

            //'flows' field is only applicable to 'oauth2' type
            if (!"oauth2".equals(type) && ValidatorUtils.flowsIsSet(t.getFlows())) {
                final String message = Tr.formatMessage(tc, "nonApplicableField", "flows", "Security Scheme Object", type);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }
        }
    }
}
