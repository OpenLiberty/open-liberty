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

        Optional<ValidationEvent> op_type = ValidatorUtils.validateRequiredField(t.getType(), "#/components/securitySchemes/" + key, "type");

        if (op_type.isPresent()) {
            op_type.ifPresent(helper::addValidationEvent);

        } else {
            if ("apiKey".equals(t.getType().toString())) {
                ValidatorUtils.validateRequiredField(t.getName(), "#/components/securitySchemes/" + key, "name").ifPresent(helper::addValidationEvent);

                Optional<ValidationEvent> op_in = ValidatorUtils.validateRequiredField(t.getIn(), "#/components/securitySchemes/" + key, "in");

                if (op_in.isPresent()) {
                    op_in.ifPresent(helper::addValidationEvent);
                } else {
                    Set<String> inValues = new HashSet<String>(Arrays.asList("query", "header", "cookie"));
                    if (!(inValues.contains(t.getIn().toString()))) {
                        final String message = Tr.formatMessage(tc, "securitySchemeInFieldInvalid", key, t.getIn().toString());
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
                    }

                }
                if (t.getBearerFormat() != null ||
                    ValidatorUtils.flowsIsSet(t.getFlows()) ||
                    t.getOpenIdConnectUrl() != null ||
                    t.getScheme() != null) {
                    final String message = Tr.formatMessage(tc, "securitySchemeNonApplicableField", t.getType().toString());
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, null, message));
                }
            } else if ("http".equals(t.getType().toString())) {
                ValidatorUtils.validateRequiredField(t.getScheme(), "#/components/securitySchemes/" + key, "scheme").ifPresent(helper::addValidationEvent);

                if (t.getOpenIdConnectUrl() != null ||
                    ValidatorUtils.flowsIsSet(t.getFlows()) ||
                    t.getName() != null ||
                    t.getIn() != null) {
                    final String message = Tr.formatMessage(tc, "securitySchemeNonApplicableField", t.getType().toString());
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, null, message));
                }

            } else if ("oauth2".equals(t.getType().toString())) {
                ValidatorUtils.validateRequiredField(t.getFlows(), "#/components/securitySchemes/" + key, "flows").ifPresent(helper::addValidationEvent);

                if (t.getOpenIdConnectUrl() != null ||
                    t.getName() != null ||
                    t.getBearerFormat() != null ||
                    t.getIn() != null ||
                    t.getScheme() != null) {
                    final String message = Tr.formatMessage(tc, "securitySchemeNonApplicableField", t.getType().toString());
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, null, message));
                }

            } else if ("openIdConnect".equals(t.getType().toString())) {
                Optional<ValidationEvent> op_url = ValidatorUtils.validateRequiredField(t.getOpenIdConnectUrl(), "#/components/securitySchemes/" + key, "openIdConnectUrl");

                if (op_url.isPresent()) {
                    op_url.ifPresent(helper::addValidationEvent);

                } else {
                    if (!(ValidatorUtils.isValidURL(t.getOpenIdConnectUrl()))) {
                        final String message = Tr.formatMessage(tc, "securitySchemeInvalidURL", t.getOpenIdConnectUrl());
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
                    }
                }
                if (t.getBearerFormat() != null ||
                    ValidatorUtils.flowsIsSet(t.getFlows()) ||
                    t.getName() != null ||
                    t.getIn() != null ||
                    t.getScheme() != null) {
                    final String message = Tr.formatMessage(tc, "securitySchemeNonApplicableField", t.getType().toString());
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, null, message));
                }
            }
        }
    }
}
