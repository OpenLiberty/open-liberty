/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.restfulWS30.cdi30.fat.lifecyclemismatch;

public class RequestScopedException extends Exception {

    private static final long serialVersionUID = -1975560538784455458L;

    public RequestScopedException() {
        super();
    }

    public RequestScopedException(String message) {
        super(message);
    }

    public RequestScopedException(Throwable cause) {
        super(cause);
    }

    public RequestScopedException(String message, Throwable cause) {
        super(message, cause);
    }
}
