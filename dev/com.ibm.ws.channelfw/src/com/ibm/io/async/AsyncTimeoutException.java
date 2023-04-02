/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package com.ibm.io.async;

/**
 * Checked exception thrown when an asynchronous operation has been prematurely completed because it
 * timed out.
 */
public class AsyncTimeoutException extends AsyncException {
    // required SUID since this is serializable
    private static final long serialVersionUID = -5699872437960867150L;

    AsyncTimeoutException() {
        super(AsyncProperties.aio_operation_timedout, "Timeout", 0);
    }
}
