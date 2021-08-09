/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.context;

public class JordanException extends Exception {

    private static final long serialVersionUID = -1975560538784455458L;

    public JordanException() {
        super();
    }

    public JordanException(String message) {
        super(message);
    }

    public JordanException(Throwable cause) {
        super(cause);
    }

    public JordanException(String message, Throwable cause) {
        super(message, cause);
    }

}
