/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.services.validation;

import org.eclipse.microprofile.openapi.models.PathItem;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.internal.utils.LoggingUtils;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.validation.PathItemValidator;
import io.openliberty.microprofile.openapi20.internal.validation.TypeValidator;
import io.openliberty.microprofile.openapi20.internal.validation.ValidationHelper;

public class PathItem31Validator extends TypeValidator<PathItem> {

    private static final TraceComponent tc = Tr.register(PathItem31Validator.class);

    private static final PathItem31Validator INSTANCE = new PathItem31Validator();

    public static PathItem31Validator getInstance() {
        return INSTANCE;
    }

    private PathItem31Validator() {}

    @Override
    public void validate(ValidationHelper helper, Context context, String key, PathItem t) {
        if (t != null) {
            String ref = t.getRef();
            if (ref != null && !ref.isEmpty()) {
                helper.validateReference(context, key, ref, PathItem.class);
                return;
            }

            if (key == null) {
                // Path can be null if user has put it within a schema, don't validate in this case.
                if (LoggingUtils.isDebugEnabled(tc)) {
                    Tr.debug(tc, "Path is null. Skip validation.");
                }
                return;
            }

            if (key.contains("{$")) {
                //Path within a Callback can contain variables (e.g. {$request.query.callbackUrl}/data ) which shouldn't be validated since they are not path params
                if (LoggingUtils.isDebugEnabled(tc)) {
                    Tr.debug(tc, "Path contains variables. Skip validation: " + key);
                }
                return;
            }

            PathItemValidator.getInstance().validateParameters(helper, context, key, t);
        }
    }

}
