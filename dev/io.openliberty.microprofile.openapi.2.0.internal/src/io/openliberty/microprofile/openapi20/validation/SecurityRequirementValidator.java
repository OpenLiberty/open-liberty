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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.Constants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;

/**
 *
 */
public class SecurityRequirementValidator extends TypeValidator<SecurityRequirement> {

    private static final TraceComponent tc = Tr.register(SecurityRequirementValidator.class);

    private static final SecurityRequirementValidator INSTANCE = new SecurityRequirementValidator();

    public static SecurityRequirementValidator getInstance() {
        return INSTANCE;
    }

    private SecurityRequirementValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, SecurityRequirement t) {

        Map<String, List<String>> secReqSchemes = t.getSchemes();
        if (secReqSchemes != null && !secReqSchemes.isEmpty()) {
            Map<String, SecurityScheme> schemes = null;
            if (context.getModel().getComponents() != null) {
                schemes = context.getModel().getComponents().getSecuritySchemes();
            }
            Set<String> h = secReqSchemes.keySet();
            for (String name : h) {
                if (schemes == null || !schemes.containsKey(name) || schemes.get(name) == null) {
                    final String message = Tr.formatMessage(tc, ValidationMessageConstants.SECURITY_REQ_NOT_DECLARED, name);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                } else {
                    String type = schemes.get(name).getType() != null ? schemes.get(name).getType().toString() : null;
                    List<String> value = secReqSchemes.get(name);
                    if ("oauth2".equals(type) || "openIdConnect".equals(type)) {
                        if (value == null || value.isEmpty()) {
                            final String message = Tr.formatMessage(tc, ValidationMessageConstants.SECURITY_REQ_SCOPE_NAMES_REQUIRED, name);
                            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                        }
                    } else if ("apiKey".equals(type) || Constants.PROTOCOL_HTTP.equals(type)) {
                        if (value != null && !value.isEmpty()) {
                            final String message = Tr.formatMessage(tc, ValidationMessageConstants.SECURITY_REQ_FIELD_NOT_EMPTY, name, value);
                            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                        }
                    }
                }

            }

        } else {
            final String message = Tr.formatMessage(tc, ValidationMessageConstants.SECURITY_REQ_IS_EMPTY);
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
        }
    }
}
