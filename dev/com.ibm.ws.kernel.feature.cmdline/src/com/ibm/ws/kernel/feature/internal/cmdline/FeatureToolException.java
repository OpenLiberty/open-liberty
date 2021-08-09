/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.cmdline;

/**
 *
 */
public class FeatureToolException extends RuntimeException {
    /**  */
    private static final long serialVersionUID = 6441973779054340565L;

    private ReturnCode returnCode = ReturnCode.RUNTIME_EXCEPTION;
    private final String translatedMsg;

    public FeatureToolException(String message, String translatedMsg) {
        super(message);
        this.translatedMsg = translatedMsg;
    }

    public FeatureToolException(String message, String translatedMsg, Throwable cause) {
        super(message, cause);
        this.translatedMsg = translatedMsg;
    }

    public FeatureToolException(String message, String translatedMsg, Throwable cause, ReturnCode rc) {
        super(message, cause);
        this.translatedMsg = translatedMsg;
        this.returnCode = rc;
    }

    public ReturnCode getReturnCode() {
        return returnCode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Throwable#getLocalizedMessage()
     */
    @Override
    public String getLocalizedMessage() {
        if (translatedMsg == null)
            return getMessage();

        return translatedMsg;
    }
}
