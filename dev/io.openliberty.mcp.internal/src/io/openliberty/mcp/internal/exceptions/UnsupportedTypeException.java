/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.exceptions;

import java.lang.reflect.Type;

/**
 * Thrown when return type is not an object and structured content is set to true as it depends on the root type of the output schema.
 *
 */
public class UnsupportedTypeException extends RuntimeException {
    /**
     * The unsupported type;
     */
    private Type type;

    public UnsupportedTypeException(Type type) {
        super();
        this.type = type;

    }

    public UnsupportedTypeException() {
        super();

    }

    /**
     * @return the type
     */
    public Type getType() {
        return this.type;
    }

    private static final long serialVersionUID = 1L;

}
