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
package com.ibm.ws.wsat.service;

/**
 * Subclass of WSATException where a specific spec-defined fault is defined
 */
public class WSATFaultException extends WSATException {
    private static final long serialVersionUID = 1L;

    private final WSATFault fault;

    public WSATFaultException(WSATFault fault) {
        super(fault.getReason());
        this.fault = fault;
    }

    public WSATFault getFault() {
        return fault;
    }

}
