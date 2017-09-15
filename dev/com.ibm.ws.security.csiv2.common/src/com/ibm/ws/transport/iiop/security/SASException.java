/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security;

/**
 * @version $Revision: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
public class SASException extends Exception {

    private final int major;

    public SASException(int major) {
        this.major = major;
    }

    public SASException(int major, Throwable cause) {
        super(cause);

        this.major = major;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return 1;
    }

    public byte[] getErrorToken() {
        // TODO: Write an error token
        return new byte[0];
    }
}
