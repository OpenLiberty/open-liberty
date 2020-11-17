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

package com.ibm.ws.microprofile.opentracing.jaeger.adapter;

/**
 *
 */
public class JaegerAdapterException extends RuntimeException {

    /**  */
    private static final long serialVersionUID = 8401273064700931173L;

    /**
     * @param cause
     */
    public JaegerAdapterException(Exception cause) {
        super(cause);
    }
    
    public JaegerAdapterException(String message) {
        super(message);
    }

}
