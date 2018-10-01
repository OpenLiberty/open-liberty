/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

public class MyRuntimeException extends RuntimeException {

    /**  */
    private static final long serialVersionUID = 1L;

    public MyRuntimeException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public MyRuntimeException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

}
