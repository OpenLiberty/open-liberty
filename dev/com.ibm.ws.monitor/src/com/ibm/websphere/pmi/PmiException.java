/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.pmi;

/**
 * @ibm-api
 */
public class PmiException extends java.lang.Exception {

    private static final long serialVersionUID = 6554934609377950521L;

    /**
     * PmiException may be thrown by PmiClient if something goes wrong.
     */
    public PmiException(String s) {
        super(s);
    }

    /**
     * PmiException may be thrown by PmiClient if something goes wrong.
     */
    public PmiException() {
        super();
    }
}
