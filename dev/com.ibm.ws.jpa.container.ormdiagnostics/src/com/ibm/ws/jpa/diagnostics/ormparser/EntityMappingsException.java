/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.diagnostics.ormparser;

public class EntityMappingsException extends Exception {
    private static final long serialVersionUID = 4424645080755882871L;

    public EntityMappingsException() {

    }

    public EntityMappingsException(String message) {
        super(message);

    }

    public EntityMappingsException(Throwable cause) {
        super(cause);
    }

    public EntityMappingsException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityMappingsException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
