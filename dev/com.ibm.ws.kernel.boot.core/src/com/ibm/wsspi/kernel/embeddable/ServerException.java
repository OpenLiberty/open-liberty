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
package com.ibm.wsspi.kernel.embeddable;

/**
 * Exception that encapsulates an issue encountered by the runtime
 * while processing a server operation.
 */
public abstract class ServerException extends RuntimeException {

    private static final long serialVersionUID = -2314997212544086815L;

    private final String translatedMsg;

    public ServerException(String message, String translatedMsg, Throwable cause) {
        super(message, cause);
        this.translatedMsg = translatedMsg;
    }

    public ServerException(String message, String translatedMsg) {
        super(message);
        this.translatedMsg = translatedMsg;
    }

    public String getTranslatedMessage() {
        return translatedMsg;
    }

}
