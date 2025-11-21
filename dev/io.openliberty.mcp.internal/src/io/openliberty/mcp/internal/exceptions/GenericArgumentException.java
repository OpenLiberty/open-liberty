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

import java.util.List;

/**
 * Thrown when arguments in a tool method contain generic components as these can't be instantiated using jsonb
 *
 */
public class GenericArgumentException extends RuntimeException {
    /**
     * List of argument names that are generic;
     */
    private List<String> arguments;

    public GenericArgumentException(List<String> arguments) {
        super();
        this.arguments = arguments;

    }

    public GenericArgumentException() {
        super();

    }

    /**
     * @return the arguments
     */
    public List<String> getArguments() {
        return arguments;
    }

    private static final long serialVersionUID = 1L;

}
