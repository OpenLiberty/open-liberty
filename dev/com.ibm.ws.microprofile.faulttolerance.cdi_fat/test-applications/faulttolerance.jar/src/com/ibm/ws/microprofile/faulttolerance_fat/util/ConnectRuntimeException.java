/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.microprofile.faulttolerance_fat.util;

/**
 * Added to enable more flexibility to use Lambdas for Supplier<Connections>
 */
public class ConnectRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConnectRuntimeException(Exception e) {
        super("ConnectException: " + e);
    }

}
