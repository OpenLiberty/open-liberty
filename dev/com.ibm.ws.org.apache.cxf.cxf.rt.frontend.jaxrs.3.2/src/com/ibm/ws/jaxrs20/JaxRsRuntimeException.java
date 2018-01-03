
/*******************************************************************************
 * Copyright (c) 2014, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20;

/**
 * Exception initially intended to allow a ServletExeption nested inside to flow out of CXF code
 * to the IBMRestServlet. But it may be used in other situations in the future.
 */
public class JaxRsRuntimeException extends RuntimeException {

    public JaxRsRuntimeException(Throwable ex) {
        super(ex);
    }
}
