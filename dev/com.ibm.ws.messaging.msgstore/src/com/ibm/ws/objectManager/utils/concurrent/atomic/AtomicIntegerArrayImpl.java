package com.ibm.ws.objectManager.utils.concurrent.atomic;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * Native JVM implementation.
 */
public class AtomicIntegerArrayImpl
                extends java.util.concurrent.atomic.AtomicIntegerArray
                implements AtomicIntegerArray {

    private static final long serialVersionUID = 1L;

    public AtomicIntegerArrayImpl(int intValue) {
        super(intValue);
    }
} // class AtomicIntegerArray.