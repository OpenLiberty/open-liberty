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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class OpenAPIValidator extends TypeValidator<OpenAPI> {

    private static final TraceComponent tc = Tr.register(OpenAPIValidator.class);

    private static final OpenAPIValidator INSTANCE = new OpenAPIValidator();

    public static OpenAPIValidator getInstance() {
        return INSTANCE;
    }

    private OpenAPIValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, OpenAPI t) {
        if (t != null) {
            String openapiVersion = t.getOpenapi();
            ValidatorUtils.validateRequiredField(openapiVersion, context, "openapi").ifPresent(helper::addValidationEvent);
            ValidatorUtils.validateRequiredField(t.getInfo(), context, "info").ifPresent(helper::addValidationEvent);
            ValidatorUtils.validateRequiredField(t.getPaths(), context, "paths").ifPresent(helper::addValidationEvent);

            if (openapiVersion != null && !openapiVersion.startsWith("3.")) {
                final String message = Tr.formatMessage(tc, "openAPIVersionInvalid", openapiVersion);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
            }

            List<Tag> tags = t.getTags();
            if (tags != null) {
                Set<String> tagNames = new HashSet<String>();
                for (Tag tag : tags) {
                    if (!tagNames.add(tag.getName())) {
                        final String message = Tr.formatMessage(tc, "openAPITagIsNotUnique", tag.getName());
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, null, message));
                    }
                }
            }
        }
    }
}
