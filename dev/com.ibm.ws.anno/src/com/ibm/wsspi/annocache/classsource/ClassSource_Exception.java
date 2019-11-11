/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.annocache.classsource;

public class ClassSource_Exception extends com.ibm.wsspi.anno.classsource.ClassSource_Exception {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("hiding")
	public static final String CLASS_NAME = "ClassSource_Exception";

    //

    public ClassSource_Exception(String message) {
        super(message);
    }

    public ClassSource_Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
