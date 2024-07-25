/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult.ValidationEvent;
import io.openliberty.microprofile.openapi20.internal.utils.ValidationMessageConstants;
import io.smallrye.openapi.runtime.io.definition.DefinitionConstant;

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
            ValidatorUtils.validateRequiredField(openapiVersion, context, DefinitionConstant.PROP_OPENAPI).ifPresent(helper::addValidationEvent);
            ValidatorUtils.validateRequiredField(t.getInfo(), context, DefinitionConstant.PROP_INFO).ifPresent(helper::addValidationEvent);
            ValidatorUtils.validateRequiredField(t.getPaths(), context, DefinitionConstant.PROP_PATHS).ifPresent(helper::addValidationEvent);

            if (openapiVersion != null && !openapiVersion.startsWith("3.")) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.OPENAPI_VERSION_INVALID, openapiVersion);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }

            List<Tag> tags = t.getTags();
            if (tags != null) {
                Set<String> tagNames = new HashSet<>();
                for (Tag tag : tags) {
                    if (!tagNames.add(tag.getName())) {
                        final String message = Tr.formatMessage(tc, ValidationMessageConstants.OPENAPI_TAG_IS_NOT_UNIQUE, tag.getName());
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                    }
                }
            }
        }
    }
}
