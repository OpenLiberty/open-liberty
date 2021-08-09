/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
    public RecoveryFailedException() {
        super();
    }

    public RecoveryFailedException(Throwable cause) {
        super(cause);
    }

    public RecoveryFailedException(String msg) {
        super(msg);
    }
}
