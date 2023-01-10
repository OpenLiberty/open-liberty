/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.cdi;

import com.ibm.ws.exception.WsException;

public class CDIException extends WsException {

    private static final long serialVersionUID = 5729749912023008025L;

    public CDIException(String message) {
        super(message);
    }

    public CDIException(Throwable t) {
        super(t);
    }

    public CDIException(String message, Throwable t) {
        super(message, t);
    }

}
