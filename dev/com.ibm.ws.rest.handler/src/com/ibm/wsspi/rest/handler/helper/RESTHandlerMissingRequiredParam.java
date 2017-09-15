/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.rest.handler.helper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This error is thrown by the RESTHandlerContainer when a request is missing a required parameter.
 * 
 * @ibm-spi
 */
public class RESTHandlerMissingRequiredParam extends RESTHandlerUserError {

    private static final long serialVersionUID = -3647481857680022528L;
    private static final TraceComponent tc = Tr.register(RESTHandlerMissingRequiredParam.class);

    public RESTHandlerMissingRequiredParam(String param) {
        super(Tr.formatMessage(tc, "MISSING_PARAMETER", param));
    }

}
