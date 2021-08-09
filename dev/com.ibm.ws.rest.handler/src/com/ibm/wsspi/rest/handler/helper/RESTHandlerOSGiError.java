/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
 * This error is thrown by the RESTHandlerContainer when an OSGi service is not bound.
 * 
 * @ibm-spi
 */
public class RESTHandlerOSGiError extends RESTHandlerInternalError {

    private static final long serialVersionUID = -3647481857680022528L;
    private static final TraceComponent tc = Tr.register(RESTHandlerOSGiError.class);

    private int statusCode = 500;

    public RESTHandlerOSGiError(String missingOSGiService) {
        super(Tr.formatMessage(tc, "OSGI_SERVICE_ERROR", missingOSGiService));
    }

    @Override
    public void setStatusCode(int code) {
        statusCode = code;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }
}
