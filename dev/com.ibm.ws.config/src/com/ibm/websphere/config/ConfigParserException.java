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

package com.ibm.websphere.config;

/**
 * An exception representing an error occurred while parsing configuration
 * documents.
 */
public class ConfigParserException extends Exception {
    private static final long serialVersionUID = -8341749732382155484L;

    public ConfigParserException() {
        super();
    }

    public ConfigParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigParserException(String message) {
        super(message);
    }

    public ConfigParserException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
