/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.businessExceptionApp;

/**
 *
 */
public class UnwrappedBusinessException extends RuntimeException {

    private static final long serialVersionUID = 6214164159077697693L;

    public UnwrappedBusinessException(String message) {
        super(message);
    }

}
