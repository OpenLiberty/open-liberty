/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.cdi12.fat.complex;

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
