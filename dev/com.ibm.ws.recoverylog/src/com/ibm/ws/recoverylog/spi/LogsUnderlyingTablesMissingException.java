/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.recoverylog.spi;

/**
 * This exception indicates that an operation on a peer recovery log has failed due to
 * the absence of underlying SQL tables.
 */
public class LogsUnderlyingTablesMissingException extends InternalLogException {
    String reason = null;

    public LogsUnderlyingTablesMissingException() {
        this(null);
    }

    public LogsUnderlyingTablesMissingException(Throwable cause) {
        super(cause);
    }

    public LogsUnderlyingTablesMissingException(String s, Throwable cause) {
        super(s, cause);
        reason = s;
    }

    @Override
    public String toString() {
        if (reason != null)
            return reason + ", " + super.toString();
        else
            return super.toString();
    }
}