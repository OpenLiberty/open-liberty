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

import org.eclipse.microprofile.openapi.models.media.Schema;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class SchemaValidator extends TypeValidator<Schema> {

    private static final TraceComponent tc = Tr.register(SchemaValidator.class);

    private static final SchemaValidator INSTANCE = new SchemaValidator();

    public static SchemaValidator getInstance() {
        return INSTANCE;
    }

    private SchemaValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Schema t) {

        String reference = t.getRef();

        if (reference != null && !reference.isEmpty()) {
            ValidatorUtils.referenceValidatorHelper(reference, t, helper, context, key);
            return;
        }

        // TODO Auto-generated method stub

    }
}
