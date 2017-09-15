/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.ee8;

/**
 *
 */
public class Http2PushException extends Exception {

    /**
     * @param string
     */
    public Http2PushException(String msg) {
        super(msg);
    }

    /**  */
    private static final long serialVersionUID = 1L;

}
