/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.validation;

import java.util.List;

import org.eclipse.microprofile.openapi.models.servers.ServerVariable;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent;
import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent.Severity;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.internal.validation.TypeValidator;
import io.openliberty.microprofile.openapi20.internal.validation.ValidationHelper;
import io.openliberty.microprofile.openapi20.internal.validation.ValidatorUtils;

public class ServerVariable31Validator extends TypeValidator<ServerVariable> {
    private static final TraceComponent tc = Tr.register(ServerVariable31Validator.class);

    private static final String PROP_DEFAULT = "default";

    private static final ServerVariable31Validator INSTANCE = new ServerVariable31Validator();

    public static ServerVariable31Validator getInstance() {
        return INSTANCE;
    }

    private ServerVariable31Validator() {}

    @Override
    public void validate(ValidationHelper helper, Context context, String key, ServerVariable t) {
        ValidatorUtils.validateRequiredField(t.getDefaultValue(), context, PROP_DEFAULT).ifPresent(helper::addValidationEvent);

        String defaultValue = t.getDefaultValue();
        List<String> enumeration = t.getEnumeration();
        if (enumeration != null) {
            if (enumeration.isEmpty()) {
                // Enum must not be empty if present
                String message = Tr.formatMessage(tc, ValidationMessageConstants.SERVER_VARIABLE_ARRAY_EMPTY, "enum");
                helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
            } else if (defaultValue != null && !enumeration.contains(defaultValue)) {
                // Enum must contain default value if present
                String message = Tr.formatMessage(tc, ValidationMessageConstants.SERVER_VARIABLE_DEFAULT_NOT_IN_ENUM, t.getDefaultValue());
                helper.addValidationEvent(new ValidationEvent(Severity.ERROR, context.getLocation(), message));
            }
        }
    }

}
