/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.testapp.g3store.exception;

/**
 * @author anupag
 *
 */
public class HandleExceptionsFromgRPCService {

    InvalidArgException argException = null;
    NotFoundException nfException = null;

    public NotFoundException getNfException() {
        return nfException;
    }

    public void setNfException(NotFoundException nfException) {
        this.nfException = nfException;
    }

    public InvalidArgException getArgException() {
        return argException;
    }

    public void setArgException(InvalidArgException argException) {
        this.argException = argException;
    }

}
