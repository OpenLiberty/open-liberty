/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package com.ibm.ws.classloading.internal.util;

public abstract class ElementNotFetchedException extends Exception {
    private static final long serialVersionUID = 1L;

    public ElementNotFetchedException() {}

    public ElementNotFetchedException(String message) {
        super(message);
    }

    public ElementNotFetchedException(Throwable cause) {
        super(cause);
    }

    public ElementNotFetchedException(String message, Throwable cause) {
        super(message, cause);
    }

}
