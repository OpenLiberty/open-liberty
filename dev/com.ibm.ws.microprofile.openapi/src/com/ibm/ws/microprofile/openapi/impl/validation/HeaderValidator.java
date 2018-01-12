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

import org.eclipse.microprofile.openapi.models.headers.Header;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class HeaderValidator extends TypeValidator<Header> {

    private static final TraceComponent tc = Tr.register(HeaderValidator.class);

    private static final HeaderValidator INSTANCE = new HeaderValidator();

    public static HeaderValidator getInstance() {
        return INSTANCE;
    }

    private HeaderValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, Header t) {

        String reference = t.getRef();

        if (reference != null && !reference.isEmpty()) {
            ValidatorUtils.referenceValidatorHelper(reference, t, helper, context, key);
            return;
        }

        // validate
    }
}
