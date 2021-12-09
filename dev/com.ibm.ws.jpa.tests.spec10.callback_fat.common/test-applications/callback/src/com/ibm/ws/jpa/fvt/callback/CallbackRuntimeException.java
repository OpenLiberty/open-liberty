/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.callback;

public class CallbackRuntimeException extends RuntimeException {
    private static final long serialVersionUID = -4735489710261151529L;

    public CallbackRuntimeException() {}

    public CallbackRuntimeException(String arg0) {
        super(arg0);
    }

    public CallbackRuntimeException(Throwable arg0) {
        super(arg0);
    }

    public CallbackRuntimeException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }
}
