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

import org.eclipse.microprofile.openapi.models.Paths;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

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
            for (String path : t.keySet()) {
                if (!path.startsWith("/")) {
                    final String message = Tr.formatMessage(tc, "pathsRequiresSlash", path);
                    helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                }
            }
        }

    }
}
