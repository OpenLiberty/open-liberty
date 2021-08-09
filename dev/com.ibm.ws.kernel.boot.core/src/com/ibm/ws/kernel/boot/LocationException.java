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
 * The LocationException is used when configured (or calculated) locations
 * can not be resolved.
 * The exception message will contain information describing the condition.
 */
public class LocationException extends ServerException {
    private static final long serialVersionUID = 5567704962465063487L;

    public LocationException(String message, String translatedMsg, Throwable cause) {
        super(message, translatedMsg, cause);
    }

    public LocationException(String message, String translatedMsg) {
        super(message, translatedMsg);
    }
}
