/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.event.logging.ejb.fat.ejb.ear;

/**
 * Checked application exception thrown on purpose by the failing bean methods. The container
 * treats it as an application exception, so it is returned to the caller without any
 * error message or FFDC.
 */
public class EJBEventTestException extends Exception {

    private static final long serialVersionUID = 1L;

    public EJBEventTestException(String message) {
        super(message);
    }
}
