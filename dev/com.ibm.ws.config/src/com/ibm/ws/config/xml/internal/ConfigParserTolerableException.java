/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import com.ibm.websphere.config.ConfigParserException;

/**
 * Indicate that some sort of exception occurred while parsing, but it's cause
 * is something that we can tolerate.
 */
public class ConfigParserTolerableException extends ConfigParserException {
    private static final long serialVersionUID = -185687558103213805L;

    public ConfigParserTolerableException() {
        super();
    }

    public ConfigParserTolerableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigParserTolerableException(String message) {
        super(message);
    }

    public ConfigParserTolerableException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

}
