/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.security.wim.scim20.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.ws.security.wim.scim20.model.ErrorImpl;

public abstract class SCIMException extends Exception {

    private static final long serialVersionUID = -1591160795632407017L;

    private final Integer httpCode;
    private final String scimType;

    public SCIMException(int httpCode, String scimType, String message) {
        this(httpCode, scimType, message, null);
    }

    public SCIMException(int httpCode, String scimType, String message, Throwable cause) {
        super(message, cause);
        this.httpCode = httpCode;
        this.scimType = scimType;
    }

    public Integer getHttpCode() {
        return httpCode;
    }

    public String getScimType() {
        return scimType;
    }

    public String asJson() throws JsonProcessingException {
        ErrorImpl error = new ErrorImpl();
        error.setScimType(scimType);
        error.setDetail(getMessage());
        error.setStatus(this.httpCode);

        ObjectMapper objectMapper = new ObjectMapper(); // TODO Should probably save an instance of this.
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(error);
    }
}
