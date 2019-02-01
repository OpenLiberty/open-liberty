/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package jpa10callback;

public class CallbackRuntimeException extends RuntimeException {
    private static final long serialVersionUID = -4735489710261151529L;

    public CallbackRuntimeException() {}

    public CallbackRuntimeException(String info) {
        super(info);
    }

    public CallbackRuntimeException(Throwable t) {
        super(t);
    }

    public CallbackRuntimeException(String info, Throwable t) {
        super(info, t);
    }
}
