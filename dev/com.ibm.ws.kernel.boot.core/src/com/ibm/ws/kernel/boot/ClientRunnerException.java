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
package com.ibm.ws.kernel.boot;

/**
 * The ClientRunnerException is used when an unknown exception occurs while running
 * ClientRunner.run() that executes the main() method of the main class in a client module.
 * The exception message will contain information describing the condition.
 */
@SuppressWarnings("serial")
public class ClientRunnerException extends LaunchException {
    private final ReturnCode returnCode = ReturnCode.CLIENT_RUNNER_EXCEPTION;

    public ClientRunnerException(String message, String translatedMsg) {
        super(message, translatedMsg);
        setReturnCode(returnCode);
    }

    public ClientRunnerException(String message, String translatedMsg, Throwable cause) {
        super(message, translatedMsg, cause);
        setReturnCode(returnCode);
    }

    public ClientRunnerException(String message, String translatedMsg, Throwable cause, ReturnCode rc) {
        super(message, translatedMsg, cause);
        setReturnCode(rc);
    }

}
