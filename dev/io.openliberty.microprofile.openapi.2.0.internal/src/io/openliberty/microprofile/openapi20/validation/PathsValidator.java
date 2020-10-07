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

import java.util.Map;

import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;

/**
 *
 */
public class PathsValidator extends TypeValidator<Paths> {

    private static final TraceComponent tc = Tr.register(PathsValidator.class);

    private static final PathsValidator INSTANCE = new PathsValidator();

    public static PathsValidator getInstance() {
        return INSTANCE;
    }

    private PathsValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Paths t) {
        if (t != null) {
            boolean mapContainsInvalidKey = false;
            Map<String, PathItem> pathItems = t.getPathItems();
            if (pathItems != null) {
                for (String path : t.getPathItems().keySet()) {
                    if (path != null && !path.isEmpty()) {
                        if (!path.startsWith("/")) {
                            final String message = Tr.formatMessage(tc, ValidationMessageConstants.PATHS_REQUIRES_SLASH, path);
                            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(path), message));
                        }
                        
                        //Ensure map doesn't contain null value
                        if (t.getPathItems().get(path) == null) {
                            final String message = Tr.formatMessage(tc, ValidationMessageConstants.NULL_VALUE_IN_MAP, path);
                            helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(path), message));
                        }
                    } else {
                        mapContainsInvalidKey = true;
                    }
                } // FOR
            } // IF - pathItems != null

            //Ensure map doesn't contain an invalid key
            if (mapContainsInvalidKey) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.NULL_OR_EMPTY_KEY_IN_MAP);
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }
        }
    }
}
