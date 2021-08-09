/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import com.ibm.wsspi.kernel.embeddable.ServerException;

/**
 * The LaunchException is used when a condition occurs that prevents the
 * launcher from being able to load or start the platform and supporting
 * OSGi framework. The exception message will contain information describing the
 * condition.
 */
public class LaunchException extends ServerException {
    private static final long serialVersionUID = 2021355231888752283L;

    private ReturnCode returnCode = ReturnCode.LAUNCH_EXCEPTION;

    public LaunchException(String message, String translatedMsg) {
        super(message, translatedMsg);
    }

    public LaunchException(String message, String translatedMsg, Throwable cause) {
        super(message, translatedMsg, cause);
    }

    public LaunchException(String message, String translatedMsg, Throwable cause, ReturnCode rc) {
        super(message, translatedMsg, cause);
        this.returnCode = rc;
    }

    public void setReturnCode(ReturnCode rc) {
        returnCode = rc;
    }

    public ReturnCode getReturnCode() {
        return returnCode;
    }
}
