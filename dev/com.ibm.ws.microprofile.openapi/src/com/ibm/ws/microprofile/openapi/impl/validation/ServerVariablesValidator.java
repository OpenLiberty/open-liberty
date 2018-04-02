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

import org.eclipse.microprofile.openapi.models.servers.ServerVariables;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class ServerVariablesValidator extends TypeValidator<ServerVariables> {

    private static final TraceComponent tc = Tr.register(ServerVariablesValidator.class);

    private static final ServerVariablesValidator INSTANCE = new ServerVariablesValidator();

    public static ServerVariablesValidator getInstance() {
        return INSTANCE;
    }

    private ServerVariablesValidator() {}

    /** {@inheritDoc} */
    @Override
    public void validate(ValidationHelper helper, Context context, String key, ServerVariables t) {
        if (t != null) {
            boolean mapContainsInvalidKey = false;
            for (String k : t.keySet()) {
                if (k == null || k.isEmpty()) {
                    mapContainsInvalidKey = true;
                } else {
                    //Ensure map doesn't contain null value
                    if (t.get(k) == null) {
                        final String message = Tr.formatMessage(tc, "nullValueInMap", k);
                        helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
                    }
                }
            }

            //Ensure map doesn't contain an invalid key
            if (mapContainsInvalidKey) {
                final String message = Tr.formatMessage(tc, "nullOrEmptyKeyInMap");
                helper.addValidationEvent(new ValidationEvent(ValidationEvent.Severity.ERROR, context.getLocation(), message));
            }
        }
    }
}
