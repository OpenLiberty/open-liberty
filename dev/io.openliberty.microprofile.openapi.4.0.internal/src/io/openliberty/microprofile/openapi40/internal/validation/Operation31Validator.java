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

import org.eclipse.microprofile.openapi.models.Operation;

import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.internal.validation.OperationValidator;
import io.openliberty.microprofile.openapi20.internal.validation.TypeValidator;
import io.openliberty.microprofile.openapi20.internal.validation.ValidationHelper;

public class Operation31Validator extends TypeValidator<Operation> {

    private static final Operation31Validator INSTANCE = new Operation31Validator();

    public static Operation31Validator getInstance() {
        return INSTANCE;
    }

    private Operation31Validator() {}

    @Override
    public void validate(ValidationHelper helper, Context context, String key, Operation t) {
        if (t != null) {
            OperationValidator.getInstance().validateId(helper, context, t);
        }
    }

}
