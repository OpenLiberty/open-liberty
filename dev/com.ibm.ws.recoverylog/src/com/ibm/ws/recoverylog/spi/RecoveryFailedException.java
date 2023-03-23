/*******************************************************************************
 * Copyright (c) 2002, 2023 IBM Corporation and others.
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

//------------------------------------------------------------------------------
// Class: RecoveryFailedException
//------------------------------------------------------------------------------
/**
 * A requested operation is not available or cannot be issued in the present state
 */
@SuppressWarnings("serial")
public class RecoveryFailedException extends Exception {
    String reason = null;

    public RecoveryFailedException() {
        super();
    }

    public RecoveryFailedException(Throwable cause) {
        super(cause);
    }

    public RecoveryFailedException(String msg) {
        super(msg);
        reason = msg;
    }

    @Override
    public String toString() {
        if (reason != null)
            return reason;
        else
            return super.toString();
    }
}
