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

import org.eclipse.microprofile.openapi.models.OpenAPI;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.internal.validation.OpenAPIValidator;
import io.openliberty.microprofile.openapi20.internal.validation.TypeValidator;
import io.openliberty.microprofile.openapi20.internal.validation.ValidationHelper;
import io.openliberty.microprofile.openapi20.internal.validation.ValidatorUtils;
import io.smallrye.openapi.runtime.io.OpenAPIDefinitionIO;

public class OpenAPIDefinition31Validator extends TypeValidator<OpenAPI> {
    private static final TraceComponent tc = Tr.register(OpenAPIDefinition31Validator.class);

    private static final OpenAPIDefinition31Validator INSTANCE = new OpenAPIDefinition31Validator();

    public static OpenAPIDefinition31Validator getInstance() {
        return INSTANCE;
    }

    private OpenAPIDefinition31Validator() {}

    @Override
    public void validate(ValidationHelper helper, Context context, String key, OpenAPI t) {
        if (t != null) {
            String openapiVersion = t.getOpenapi();
            ValidatorUtils.validateRequiredField(openapiVersion, context, OpenAPIDefinitionIO.PROP_OPENAPI).ifPresent(helper::addValidationEvent);
            ValidatorUtils.validateRequiredField(t.getInfo(), context, OpenAPIDefinitionIO.PROP_INFO).ifPresent(helper::addValidationEvent);

            if (openapiVersion != null && !openapiVersion.startsWith("3.")) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.OPENAPI_VERSION_INVALID, openapiVersion);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            if (t.getPaths() == null && t.getComponents() == null && t.getWebhooks() == null) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.OPENAPI_MISSING_REQUIRED_FIELDS);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            OpenAPIValidator.getInstance().validateTags(helper, context, t);
        }
    }

}
