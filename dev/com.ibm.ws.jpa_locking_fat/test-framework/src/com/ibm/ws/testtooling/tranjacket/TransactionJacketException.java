/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.testtooling.tranjacket;

public class TransactionJacketException extends RuntimeException {
    private static final long serialVersionUID = -60484775101654711L;

    public TransactionJacketException() {}

    public TransactionJacketException(String message) {
        super(message);
    }

    public TransactionJacketException(Throwable t) {
        super(t);
    }

    public TransactionJacketException(String message, Throwable t) {
        super(message, t);
    }

}