/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.wlm;

/**
 *
 */
public class AlreadyClassifiedException extends Exception {

    private static final long serialVersionUID = 700401309561072902L; // [LIDB3706-5.46]

    public AlreadyClassifiedException() {
        super();
    }

    public AlreadyClassifiedException(String message) {
        super(message);
    }

    public AlreadyClassifiedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyClassifiedException(Throwable cause) {
        super(cause);
    }
}
