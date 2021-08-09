/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.impl.validation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

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

        if (!(t.isEmpty())) {
            Map<String, SecurityScheme> schemes = null;
            if (context.getModel().getComponents() != null) {
                schemes = context.getModel().getComponents().getSecuritySchemes();
            }
            Set<String> h = t.keySet();
            for (String name : h) {
                if (schemes == null || !schemes.containsKey(name) || schemes.get(name) == null) {
                    final String message = Tr.formatMessage(tc, "securityRequirementNotDeclared", name);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                } else {
                    String type = schemes.get(name).getType() != null ? schemes.get(name).getType().toString() : null;
                    List<String> value = t.get(name);
                    if ("oauth2".equals(type) || "openIdConnect".equals(type)) {
                        if (value == null || value.isEmpty()) {
                            final String message = Tr.formatMessage(tc, "securityRequirementScopeNamesRequired", name);
                            helper.addValidationEvent(
                                                      new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                        }
                    } else if ("apiKey".equals(type) || "http".equals(type)) {
                        if (value != null && !value.isEmpty()) {
                            final String message = Tr.formatMessage(tc, "securityRequirementFieldNotEmpty", name, value);
                            helper.addValidationEvent(
                                                      new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                        }
                    }
                }

            }

        } else {
            final String message = Tr.formatMessage(tc, "securityRequirementIsEmpty");
            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
        }
    }

}
