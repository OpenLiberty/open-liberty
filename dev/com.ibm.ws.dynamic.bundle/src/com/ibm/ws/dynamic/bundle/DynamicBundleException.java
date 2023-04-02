/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.ws.dynamic.bundle;

public class DynamicBundleException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DynamicBundleException() {}

    public DynamicBundleException(String message, Throwable cause) {
        super(message, cause);
    }

    public DynamicBundleException(String message) {
        super(message);
    }

    public DynamicBundleException(Throwable cause) {
        super(cause);
    }
}
