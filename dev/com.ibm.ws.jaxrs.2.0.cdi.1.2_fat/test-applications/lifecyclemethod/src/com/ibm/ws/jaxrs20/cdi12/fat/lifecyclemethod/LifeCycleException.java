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
package com.ibm.ws.jaxrs20.cdi12.fat.lifecyclemethod;

public class LifeCycleException extends Exception {

    private static final long serialVersionUID = -1975560538784455458L;

    public LifeCycleException() {
        super();
    }

    public LifeCycleException(String message) {
        super(message);
    }

    public LifeCycleException(Throwable cause) {
        super(cause);
    }

    public LifeCycleException(String message, Throwable cause) {
        super(message, cause);
    }
}
