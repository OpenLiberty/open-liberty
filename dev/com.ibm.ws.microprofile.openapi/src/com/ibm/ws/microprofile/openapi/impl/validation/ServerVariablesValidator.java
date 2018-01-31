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
        // TODO Auto-generated method stub
    }
}
