/*******************************************************************************
 * Copyright (c) 2002, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webservices.configuration;

/**
 */
public class WASWebServicesBindException extends Exception {

    private static final long serialVersionUID = 6989712263638394707L;

    public WASWebServicesBindException() {}

    public WASWebServicesBindException(String msg) {
        super(msg);
    }
}
