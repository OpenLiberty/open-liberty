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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;
import io.smallrye.openapi.runtime.io.securityscheme.SecuritySchemeConstant;

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

        Optional<ValidationEvent> op_type = ValidatorUtils.validateRequiredField(t.getType(), context, SecuritySchemeConstant.PROP_TYPE);

        if (op_type.isPresent()) {
            op_type.ifPresent(helper::addValidationEvent);

        } else {
            String type = t.getType().toString();
            if (SecuritySchemeType.APIKEY.toString().equals(type)) {
                ValidatorUtils.validateRequiredField(t.getName(), context, SecuritySchemeConstant.PROP_NAME).ifPresent(helper::addValidationEvent);

                Optional<ValidationEvent> op_in = ValidatorUtils.validateRequiredField(t.getIn(), context, SecuritySchemeConstant.PROP_IN);

                if (op_in.isPresent()) {
                    op_in.ifPresent(helper::addValidationEvent);
                } else {
                    // Retrieve the list of valid In values
                    List<String> inValues = Arrays.asList(SecurityScheme.In.values()).stream().map(SecurityScheme.In::toString).collect(Collectors.toList());
                    if (!(inValues.contains(t.getIn().toString()))) {
                        final String message = Tr.formatMessage(tc, ValidationMessageConstants.SECURITY_SCHEME_IN_FIELD_INVALID, key, t.getIn().toString());
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                    }

                }

            } else if (SecuritySchemeType.HTTP.toString().equals(type)) {
                ValidatorUtils.validateRequiredField(t.getScheme(), context, SecuritySchemeConstant.PROP_SCHEME).ifPresent(helper::addValidationEvent);

            } else if (SecuritySchemeType.OAUTH2.toString().equals(type)) {
                ValidatorUtils.validateRequiredField(t.getFlows(), context, SecuritySchemeConstant.PROP_FLOWS).ifPresent(helper::addValidationEvent);

            } else if (SecuritySchemeType.OPENIDCONNECT.toString().equals(type)) {
                Optional<ValidationEvent> op_url = ValidatorUtils.validateRequiredField(t.getOpenIdConnectUrl(), context, SecuritySchemeConstant.PROP_OPEN_ID_CONNECT_URL);

                if (op_url.isPresent()) {
                    op_url.ifPresent(helper::addValidationEvent);

                } else {
                    if (!(ValidatorUtils.isValidURI(t.getOpenIdConnectUrl()))) {
                        final String message = Tr.formatMessage(tc, ValidationMessageConstants.SECURITY_SCHEMA_INVALID_URL, t.getOpenIdConnectUrl());
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                    }
                }
            }

            //Issue warnings for non-applicable fields

            //'bearerFormat' field is only applicable to 'http' type
            if (t.getBearerFormat() != null && !t.getBearerFormat().isEmpty() && !SecuritySchemeType.HTTP.toString().equals(type)) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.NON_APPLICABLE_FIELD_WITH_VALUE, SecuritySchemeConstant.PROP_BEARER_FORMAT, t.getBearerFormat(), ValidationMessageConstants.VARIABLE_SECURITY_SCHEME_OBJECT, type);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }

            //'scheme' field is only applicable to 'http' type
            if (t.getScheme() != null && !t.getScheme().isEmpty() && !SecuritySchemeType.HTTP.toString().equals(type)) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.NON_APPLICABLE_FIELD_WITH_VALUE, SecuritySchemeConstant.PROP_SCHEME, t.getScheme(), ValidationMessageConstants.VARIABLE_SECURITY_SCHEME_OBJECT, type);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }

            //'in' field is only applicable to 'apiKey' type
            if (t.getIn() != null && !SecuritySchemeType.APIKEY.toString().equals(type)) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.NON_APPLICABLE_FIELD_WITH_VALUE, SecuritySchemeConstant.PROP_IN, t.getIn(), ValidationMessageConstants.VARIABLE_SECURITY_SCHEME_OBJECT, type);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }

            //'name' field is only applicable to 'apiKey' type
            if (t.getName() != null && !t.getName().isEmpty() && !SecuritySchemeType.APIKEY.toString().equals(type)) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.NON_APPLICABLE_FIELD_WITH_VALUE, SecuritySchemeConstant.PROP_NAME, t.getName(), ValidationMessageConstants.VARIABLE_SECURITY_SCHEME_OBJECT, type);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }

            //'openIdConnectUrl' field is only applicable to 'openIdConnect' type
            if (t.getOpenIdConnectUrl() != null && !t.getOpenIdConnectUrl().isEmpty() && !SecuritySchemeType.OPENIDCONNECT.toString().equals(type)) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.NON_APPLICABLE_FIELD_WITH_VALUE, SecuritySchemeConstant.PROP_OPEN_ID_CONNECT_URL, t.getOpenIdConnectUrl(), ValidationMessageConstants.VARIABLE_SECURITY_SCHEME_OBJECT, type);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }

            //'flows' field is only applicable to 'oauth2' type
            if (!SecuritySchemeType.OAUTH2.toString().equals(type) && ValidatorUtils.flowsIsSet(t.getFlows())) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.NON_APPLICABLE_FIELD, SecuritySchemeConstant.PROP_FLOWS, ValidationMessageConstants.VARIABLE_SECURITY_SCHEME_OBJECT, type);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.WARNING, context.getLocation(), message));
            }
        }
    }
}
