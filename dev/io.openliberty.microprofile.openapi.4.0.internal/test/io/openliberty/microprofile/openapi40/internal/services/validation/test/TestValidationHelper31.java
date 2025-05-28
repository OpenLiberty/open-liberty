/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.services.validation.test;

import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi40.internal.validation.Reference31Validator;

/**
 *
 */
public class TestValidationHelper31 extends TestValidationHelper {

    @Override
    public Object validateReference(Context context, String key, String ref) {
        return Reference31Validator.getInstance().validate(this, context, ref);
    }

}
